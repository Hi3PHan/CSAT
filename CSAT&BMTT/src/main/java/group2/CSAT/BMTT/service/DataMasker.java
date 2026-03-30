package group2.CSAT.BMTT.service;

/**
 * DataMasker — Xử lý che giấu dữ liệu nhạy cảm dựa trên cấp bậc người dùng.
 *
 * Các phương thức là Pure Functions (không có side effect).
 *
 * Quy tắc:
 *   maskPhone("0912345678")      -> "091****678"     (giữ 3 đầu, che 4 giữa, giữ 3 cuối)
 *   maskCCCD("079201234567")     -> "0792****4567"   (giữ 4 đầu, che 4 giữa, giữ 4 cuối)
 *   maskSalary("15000000")       -> "****0000"       (che nửa đầu)
 *   maskBankAccount("12345...")  -> "****5678901"    (che 4 đầu, giữ phần còn lại)
 *   maskEmail("abc@gmail.com")   -> "a**@gmail.com"  (che ký tự giữa local part)
 */
public class DataMasker {

    // ─────────────────────────────────────────────────────────────
    //  Public Masking Methods
    // ─────────────────────────────────────────────────────────────

    /**
     * Mask số điện thoại 10 số: giữ 3 đầu, che 4 giữa, giữ 3 cuối.
     * VD: "0912345678" --> "091****678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    /**
     * Mask CCCD 12 số: giữ 4 đầu, che 4 giữa, giữ 4 cuối.
     * VD: "079201234567" --> "0792****4567"
     */
    public static String maskCCCD(String cccd) {
        if (cccd == null || cccd.length() < 9) return "***";
        return cccd.substring(0, 4) + "****" + cccd.substring(cccd.length() - 4);
    }

    /**
     * Mask lương: che nửa đầu.
     * VD: "15000000" --> "****0000"
     */
    public static String maskSalary(String salary) {
        if (salary == null || salary.isBlank()) return "***";
        int len     = salary.length();
        int visible = len / 2;
        return "*".repeat(len - visible) + salary.substring(len - visible);
    }

    /**
     * Mask số tài khoản ngân hàng: che 4 ký tự đầu, giữ phần còn lại.
     * VD: "1234567890123" --> "****567890123"
     */
    public static String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.length() < 5) return "***";
        return "****" + bankAccount.substring(4);
    }

    /**
     * Mask email: che phần giữa của local part.
     * VD: "nguyenvana@gmail.com" --> "n*******a@gmail.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        String local  = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) return local + domain;
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + domain;
    }
}
