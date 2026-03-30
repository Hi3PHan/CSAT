package group2.CSAT.BMTT.controller;

import group2.CSAT.BMTT.model.dto.LoginRequest;
import group2.CSAT.BMTT.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — Xử lý xác thực.
 *
 * POST /api/auth/login  — Nhận username/password, trả access token.
 *
 * Lưu ý: Nếu CryptoFilter đang hoạt động (header X-Encrypted: true),
 * request body sẽ được giải mã Hex -> JSON tự động trước khi vào đây.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> result = authService.login(request);
        return ResponseEntity.ok(result);
    }
}
