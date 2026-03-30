package group2.CSAT.BMTT.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * CryptoAttributeConverter — JPA Converter tự động mã hóa/giải mã
 * các trường nhạy cảm (CCCD, SĐT, Lương, Số TK...) khi tương tác với DB.
 *
 * Cách dùng trên Entity:
 *   @Convert(converter = CryptoAttributeConverter.class)
 *   private String cccd;
 *
 * DB sẽ lưu chuỗi Hex vô nghĩa. Java code sẽ luôn nhìn thấy giá trị gốc.
 */
@Component
@Converter
public class CryptoAttributeConverter implements AttributeConverter<String, String> {

    private final AESCipher cipher;

    /**
     * Key được load từ application.properties.
     * Phải đúng 16 ký tự (AES-128).
     */
    public CryptoAttributeConverter(@Value("${app.aes.key}") String aesKey) {
        this.cipher = new AESCipher(aesKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Được gọi trước khi INSERT/UPDATE vào DB.
     * Chuyển giá trị gốc -> Hex mã hóa AES.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return attribute;
        return cipher.encrypt(attribute);
    }

    /**
     * Được gọi sau khi SELECT từ DB.
     * Chuyển Hex mã hóa AES -> giá trị gốc.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return dbData;
        return cipher.decrypt(dbData);
    }
}
