package group2.CSAT.BMTT.controller;

import group2.CSAT.BMTT.model.dto.CreateUserRequest;
import group2.CSAT.BMTT.model.dto.UserResponse;
import group2.CSAT.BMTT.model.entity.Account;
import group2.CSAT.BMTT.service.AuthService;
import group2.CSAT.BMTT.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UserController — API lấy thông tin nhân viên.
 *
 * Mọi request cần gửi header: Authorization: Bearer <access_token>
 *
 * Dữ liệu trả về sẽ được DataMasker che giấu theo Level của người gọi.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService  userService;
    private final AuthService  authService;

    /** Lấy danh sách tất cả nhân viên (đã mask theo Level) */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {

        Account caller = extractAccount(authHeader);
        return ResponseEntity.ok(userService.getAllUsers(caller));
    }

    /** Lấy chi tiết 1 nhân viên */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {

        Account caller = extractAccount(authHeader);
        return ResponseEntity.ok(userService.getUserById(id, caller));
    }

    /** Tạo user mới (chỉ giám đốc level=3). */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @RequestHeader("Authorization") String authHeader) {

        Account caller = extractAccount(authHeader);
        return ResponseEntity.ok(userService.createUser(request, caller));
    }

    // ─────────────────────────────────────────────────────────────
    //  Helper: Lấy Account từ Bearer Token header
    // ─────────────────────────────────────────────────────────────

    private Account extractAccount(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Thiếu hoặc sai format Authorization header.");
        }
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}
