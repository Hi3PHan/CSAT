package group2.CSAT.BMTT.model.dto;

import lombok.Data;

/** DTO nhận payload đăng nhập từ Java Swing Client */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
