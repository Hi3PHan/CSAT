# Hướng Dẫn Luồng Xử Lý SHA-256 (Step-by-Step Guide)

Tài liệu này giải thích quy trình băm một chuỗi văn bản bằng thuật toán SHA-256 tự cài đặt, đi từ dữ liệu thô đến chuỗi Hex cuối cùng.

---

## 1. Sơ đồ tổng quát
`Dữ liệu thô` $\rightarrow$ `Đệm (Padding)` $\rightarrow$ `Chia khối (Blocks)` $\rightarrow$ `Xử lý từng khối (Message Schedule + Compression)` $\rightarrow$ `Kết quả (Hex)`

---

## 2. Chi tiết từng bước và các hàm sử dụng

### Bước 1: Điểm bắt đầu (Entry Point)
*   **Hàm chính:** `SHA256.hashHex(String message)`
*   **Mô tả:** Nhận vào thông điệp người dùng, chuyển đổi thành mảng byte UTF-8 và bắt đầu quy trình băm.

### Bước 2: Đệm dữ liệu (Padding)
*   **Hàm chính:** `Sha256Padding.pad(byte[] message)`
*   **Logic xử lý:**
    1.  Chèn byte mã Hex `0x80` ngay sau dữ liệu gốc (đại diện cho bit 1 đầu tiên).
    2.  Tính toán và chèn thêm các byte `0x00` (bit 0) sao cho phần dữ liệu đi trước đạt đúng mốc **56 byte** lẻ so với khối 64 byte.
    3.  Chèn **8 byte cuối cùng** mô tả độ dài bit của thông điệp gốc (ví dụ: 100 byte $\rightarrow$ ghi số 800).
*   **Đầu ra:** Một mảng byte mới có độ dài vừa khít bội số của 64.

### Bước 3: Khởi tạo giá trị băm (Hash State Init)
*   **Hàm chính:** `Sha256Constants.initialHash()` (trong `Sha256Engine`)
*   **Nhiệm vụ:** Tạo ra 8 biến trạng thái ban đầu ($H_0$ đến $H_7$) dựa trên phần phân số căn bậc hai của các số nguyên tố đầu tiên.

### Bước 4: Vòng lặp xử lý khối (Loop for each 64-byte block)
Dữ liệu đã đệm sẽ được chia thành từng khối 64 byte (512 bit). Với mỗi khối, ta thực hiện:

#### 4a. Lập lịch tin nhắn (Message Schedule)
*   **Hàm chính:** `Sha256MessageSchedule.fromBlock(...)`
*   **Hàm bitwise dùng:** `sigma0Lower()`, `sigma1Lower()` (trong `Sha256BitOps`)
*   **Logic:**
    1.  Gom 64 byte của khối thành 16 từ (word) 32-bit đầu tiên ($W_0 \dots W_{15}$).
    2.  Từ 16 từ này, dùng các hàm xoay/dịch bit Sigma "thường" để mở rộng thành **64 từ** ($W_{16} \dots W_{63}$).

#### 4b. Thực hiện băm nén (Compression)
*   **Hàm chính:** `Sha256Compression.compressBlock(...)`
*   **Hàm bitwise dùng:** `sigma0Upper()`, `sigma1Upper()`, `ch()`, `maj()` (trong `Sha256BitOps`)
*   **Hằng số dùng:** `Sha256Constants.K` (mảng 64 hằng số hệ thống)
*   **Logic:**
    1.  Khởi tạo 8 biến tạm $a, b, c, d, e, f, g, h$ bằng giá trị $H$ hiện thời.
    2.  Chạy **64 vòng lặp mã hóa biến đổi**. Mỗi vòng trộn từ tin nhắn tương ứng ($W_t$) với hằng số $K_t$ và các hàm logic.
    3.  Kết thúc 64 vòng, cộng dồn kết quả $a \dots h$ vào giá trị băm $H$ của khối trước đó.

### Bước 5: Trả về kết quả (Final Output)
*   **Hàm chính:** `Sha256Hex.toHex(int[] hashState)`
*   **Logic:** Nối 8 giá trị $H_0 \dots H_7$ sau khi đã băm xong tất cả các khối và chuyển chúng thành chuỗi Hexadecimal 64 ký tự.

---

## 3. Bảng tóm tắt các phép toán Bitwise (`Sha256BitOps`)

| Hàm | Mục đích | Sử dụng tại |
| :--- | :--- | :--- |
| `rightRotate` | Xoay bit sang phải | Hàm Sigma |
| `ch` | Lựa chọn bit (Choice) | 64 vòng nén |
| `maj` | Chọn bit đa số (Majority) | 64 vòng nén |
| `sigma[0/1]Upper` | Biến đổi bit chữ hoa ($\Sigma$) | 64 vòng nén |
| `sigma[0/1]Lower` | Biến đổi bit chữ thường ($\sigma$) | Mở rộng tin nhắn (W) |
