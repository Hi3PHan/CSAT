package group2.CSAT.BMTT.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** DTO nhận payload tạo employee mới. */
@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateUserRequest {

    @NotBlank(message = "fullName khong duoc de trong")
    @Size(max = 100, message = "fullName toi da 100 ky tu")
    private String fullName;

    @NotBlank(message = "cccd khong duoc de trong")
    @Pattern(regexp = "^\\d{12}$", message = "cccd phai gom dung 12 chu so")
    private String cccd;

    @NotBlank(message = "phone khong duoc de trong")
    @Pattern(regexp = "^\\d{10,11}$", message = "phone phai gom 10-11 chu so")
    private String phone;

    @NotBlank(message = "email khong duoc de trong")
    @Email(message = "email khong dung dinh dang")
    @Size(max = 120, message = "email toi da 120 ky tu")
    private String email;

    @NotBlank(message = "bankAccount khong duoc de trong")
    @Pattern(regexp = "^\\d{8,20}$", message = "bankAccount phai gom 8-20 chu so")
    private String bankAccount;

    @NotBlank(message = "salary khong duoc de trong")
    @Pattern(regexp = "^\\d{1,12}$", message = "salary phai la so nguyen duong")
    private String salary;
}

