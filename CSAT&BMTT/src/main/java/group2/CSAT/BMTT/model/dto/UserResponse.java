package group2.CSAT.BMTT.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO trả về thông tin User đã được Masking theo Level.
 * KHÔNG BAO GIỜ trả Entity thô ra ngoài.
 *
 * Masking rule:
 *   Level 1 (Nhân viên)    : cccd, phone, salary, bankAccount đều bị mask
 *   Level 2 (Trưởng phòng) : salary, bankAccount bị mask
 *   Level 3 (Giám đốc)     : Xem đầy đủ, không mask
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long   id;
    private String fullName;
    private String cccd;
    private String phone;
    private String email;
    private String bankAccount;
    private String salary;
    private String algo;         // Cho biết thuật toán mã hóa DB đã dùng
}
