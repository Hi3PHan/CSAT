package group2.CSAT.BMTT.service;

import group2.CSAT.BMTT.model.dto.CreateUserRequest;
import group2.CSAT.BMTT.model.dto.UserResponse;
import group2.CSAT.BMTT.model.entity.Account;
import group2.CSAT.BMTT.model.entity.UserBusiness;
import group2.CSAT.BMTT.repository.UserBusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService — Lấy danh sách User và áp dụng Masking theo Level.
 *
 * Dữ liệu từ DB sẽ được CryptoAttributeConverter tự động giải mã AES lên
 * khi vào Entity. UserService chỉ cần áp dụng Masking tùy Level:
 *
 *   Level 1 (Nhân viên)    : mask CCCD, Phone, Salary, BankAccount, Email
 *   Level 2 (Trưởng phòng) : mask Salary, BankAccount
 *   Level 3 (Giám đốc)     : Không mask gì — xem đầy đủ
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_ALGO = "AES-128-ECB";

    private final UserBusinessRepository userRepository;

    public List<UserResponse> getAllUsers(Account caller) {
        List<UserBusiness> users = userRepository.findAll();
        return users.stream()
                .map(user -> toResponse(user, caller.getLevel()))
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id, Account caller) {
        UserBusiness user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User id=" + id));
        return toResponse(user, caller.getLevel());
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request, Account caller) {
        if (caller.getLevel() == null || caller.getLevel() != 3) {
            throw new RuntimeException("Chỉ giám đốc (level 3) mới được tạo user.");
        }

        UserBusiness saved = userRepository.save(UserBusiness.builder()
                .fullName(request.getFullName().trim())
                .cccd(request.getCccd().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail().trim())
                .bankAccount(request.getBankAccount().trim())
                .salary(request.getSalary().trim())
                .algo(DEFAULT_ALGO)
                .build());

        return toResponse(saved, caller.getLevel());
    }

    // ─────────────────────────────────────────────────────────────
    //  Mapping Entity -> DTO + Masking
    // ─────────────────────────────────────────────────────────────

    private UserResponse toResponse(UserBusiness user, int level) {
        // Dữ liệu đã được JPA Converter giải mã AES ra đây
        String fullName    = user.getFullName();
        String cccd        = user.getCccd();
        String phone       = user.getPhone();
        String email       = user.getEmail();
        String bankAccount = user.getBankAccount();
        String salary      = user.getSalary();

        if (level == 3) {
            // Giám đốc: Xem full, không mask gì
            return UserResponse.builder()
                    .id(user.getId())
                    .fullName(fullName)
                    .cccd(cccd)
                    .phone(phone)
                    .email(email)
                    .bankAccount(bankAccount)
                    .salary(salary)
                    .algo(user.getAlgo())
                    .build();
        } else if (level == 2) {
            // Trưởng phòng: Mask Salary + BankAccount
            return UserResponse.builder()
                    .id(user.getId())
                    .fullName(fullName)
                    .cccd(cccd)
                    .phone(phone)
                    .email(email)
                    .bankAccount(DataMasker.maskBankAccount(bankAccount))
                    .salary(DataMasker.maskSalary(salary))
                    .algo(user.getAlgo())
                    .build();
        } else {
            // Level 1 (Nhân viên): Mask hầu hết các trường nhạy cảm
            return UserResponse.builder()
                    .id(user.getId())
                    .fullName(fullName)
                    .cccd(DataMasker.maskCCCD(cccd))
                    .phone(DataMasker.maskPhone(phone))
                    .email(DataMasker.maskEmail(email))
                    .bankAccount(DataMasker.maskBankAccount(bankAccount))
                    .salary(DataMasker.maskSalary(salary))
                    .algo(user.getAlgo())
                    .build();
        }
    }
}
