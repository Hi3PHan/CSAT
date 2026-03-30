package group2.CSAT.BMTT.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bảng accounts: Quản lý tài khoản đăng nhập.
 * - password_hash: Lưu hash do CustomSHA256PasswordEncoder tạo, KHÔNG dùng AES.
 * - level: Phân quyền 1 (Nhân viên) / 2 (Trưởng phòng) / 3 (Giám đốc).
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Cấp bậc phân quyền:
     * 1 = Nhân viên    : Chỉ xem data được mask nhiều nhất
     * 2 = Trưởng phòng : Xem ít bị mask hơn
     * 3 = Giám đốc     : Xem toàn bộ thông tin gốc
     */
    @Column(nullable = false)
    private Integer level;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
