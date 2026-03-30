package group2.CSAT.BMTT.model.entity;

import group2.CSAT.BMTT.crypto.CryptoAttributeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bảng users: Lưu thông tin nhạy cảm của nhân viên.
 *
 * Tất cả các trường nhạy cảm đều dùng @Convert(converter = CryptoAttributeConverter.class).
 * JPA sẽ tự động:
 *   - Gọi AESCipher.encrypt() trước khi INSERT/UPDATE vào DB.
 *   - Gọi AESCipher.decrypt() sau khi SELECT từ DB.
 *
 * DB thực tế lưu chuỗi Hex vô nghĩa (VD: "a3f4c8...").
 * Java code luôn thấy giá trị gốc (VD: "Nguyen Van A").
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Convert(converter = CryptoAttributeConverter.class)
    @Column(name = "full_name", nullable = false, columnDefinition = "TEXT")
    private String fullName;

    @Convert(converter = CryptoAttributeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String cccd;

    @Convert(converter = CryptoAttributeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = CryptoAttributeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String email;

//    @Convert(converter = CryptoAttributeConverter.class)
    @Column(name = "bank_account", nullable = false, columnDefinition = "TEXT")
    private String bankAccount;

    /** Lưu số lương dưới dạng chuỗi (VD: "15000000") rồi mã hóa */
    @Convert(converter = CryptoAttributeConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String salary;

    /** Ghi lại thuật toán mã hóa đã dùng (VD: "AES-128-ECB") */
    @Column(nullable = false, length = 50)
    private String algo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
}
