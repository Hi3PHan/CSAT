package group2.CSAT.BMTT.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Bảng sessions: Quản lý phiên đăng nhập.
 *
 * Hai loại token:
 * - challenge : Mã dùng một lần, thời hạn 3 phút để chống Replay Attack.
 *              user_id = null (chưa xác thực).
 * - access    : Mã phiên làm việc, thời hạn 30 phút, gắn với user_id.
 */
@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false)
    private TokenType tokenType;

    /** null khi tokenType = challenge (chưa có userId) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Account user;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum TokenType {
        challenge, access
    }
}
