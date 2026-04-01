package group2.CSAT.BMTT.security;

import group2.CSAT.BMTT.algorithm.SHA256;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import group2.CSAT.BMTT.algorithm.CustomSaltGenerator;

/**
 * Lớp cấu hình tự viết thay thế cho BCryptPasswordEncoder.
 * Sử dụng giải thuật SHA-256 tự cài từ đầu (bằng toán học, không xài builtin library).
 * 
 * Để bảo mật tốt hơn, lớp này vẫn tự sinh thêm giá trị muối (Salt)
 * Format chuỗi băm lưu xuống DB sẽ là: {salt-base64}${chuỗi-hex-sha256}
 */
public class CustomSHA256PasswordEncoder implements PasswordEncoder {

    // Sinh chuỗi ngẫu nhiên (sử dụng generator tự viết)
    /**
     * Hàm encode: Băm mật khẩu người dùng truyền gửi vào
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null");
        }

        // 1. Sinh ngẫu nhiên "Salt" dựa trên 3 lớp (Time, Random, Counter)
        // Thay thế Base64 bằng mã hóa Base36 và tự sinh entropy
        String salt = CustomSaltGenerator.generateSalt();

        // 2. Trộn Salt vào Mật khẩu (Muối + Mật khẩu hiện tại)
        String saltedPassword = salt + rawPassword;

        // 3. Tiến hành gọi hàm SHA-256 ta tự viết trước đó
        String hashHex = SHA256.hashHex(saltedPassword);

        // 4. Trả kết quả kèm phân cách $ để phân biệt thành phần
        return salt + "$" + hashHex;
    }

    /**
     * Hàm matches: Kiểm tra mật khẩu do người dùng điền so sánh với DB
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            return false;
        }

        // Kiểm tra xem chuỗi có lưu đúng format không
        if (encodedPassword == null || !encodedPassword.contains("$")) {
            return false;
        }

        // Tách chuỗi ra làm 2 phần: [0] = Salt, [1] = Chuỗi SHA256_Hash
        String[] parts = encodedPassword.split("\\$", 2);
        if (parts.length != 2) return false;

        String salt = parts[0];
        String savedHashHex = parts[1];

        // Lấy lại muối cũ ráp vào chuỗi mật khẩu người dùng hiện nhập vào
        String testPassword = salt + rawPassword;
        
        // Gọi lại thuật toán SHA-256 tự code để sinh ra chuỗi test_hash
        String testHashHex = SHA256.hashHex(testPassword);

        // So sánh constant-time để tránh timing side-channel.
        return MessageDigest.isEqual(
                savedHashHex.getBytes(StandardCharsets.UTF_8),
                testHashHex.getBytes(StandardCharsets.UTF_8)
        );
    }
}
