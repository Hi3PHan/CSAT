package group2.CSAT.BMTT.service;

import group2.CSAT.BMTT.model.entity.Account;
import group2.CSAT.BMTT.model.entity.Session;
import group2.CSAT.BMTT.model.dto.LoginRequest;
import group2.CSAT.BMTT.repository.AccountRepository;
import group2.CSAT.BMTT.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AuthService — Xử lý xác thực người dùng.
 *
 * - Login: So khớp hash từ CustomSHA256PasswordEncoder (salt + SHA-256).
 * - Access Token: Token ngẫu nhiên UUID có thời hạn 30 phút trong bảng sessions.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository  accountRepository;
    private final SessionRepository  sessionRepository;
    private final PasswordEncoder    passwordEncoder;

    /**
     * Xác thực đăng nhập.
     *
     * @return Map chứa "token" (access token) và "level"
     * @throws RuntimeException nếu sai username hoặc password
     */
    @Transactional
    public Map<String, Object> login(LoginRequest request) {
        Account account = accountRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập không tồn tại."));

        // Password hash không decrypt; chỉ có thể verify bằng matches().
        boolean matches = passwordEncoder.matches(request.getPassword(), account.getPasswordHash());
        if (!matches) {
            throw new RuntimeException("Mật khẩu không đúng.");
        }

        // Tạo access token mới, thời hạn 30 phút
        String token = UUID.randomUUID().toString();
        Session session = Session.builder()
                .token(token)
                .tokenType(Session.TokenType.access)
                .user(account)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        sessionRepository.save(session);

        return Map.of(
            "token",    token,
            "level",    account.getLevel(),
            "username", account.getUsername()
        );
    }

    /**
     * Kiểm tra và lấy Account từ access token.
     *
     * @return Account tương ứng với token
     * @throws RuntimeException nếu token không hợp lệ hoặc đã hết hạn
     */
    public Account validateToken(String token) {
        Session session = sessionRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token không hợp lệ."));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.deleteByToken(token);
            throw new RuntimeException("Phiên đã hết hạn. Vui lòng đăng nhập lại.");
        }
        if (session.getTokenType() != Session.TokenType.access) {
            throw new RuntimeException("Token không đúng loại.");
        }
        return session.getUser();
    }
}
