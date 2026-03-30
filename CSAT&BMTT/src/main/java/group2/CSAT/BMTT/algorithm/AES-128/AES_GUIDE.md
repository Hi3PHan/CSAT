# Hướng dẫn toán học: AES-128 từ đầu

> Chuẩn: **NIST FIPS 197**  
> Key size: **128 bit** – Block size: **128 bit** – Số vòng: **10**

---

## 1. Cấu trúc tổng quan

AES xử lý dữ liệu trên một **state** – ma trận 4×4 byte (cột trước, hàng sau):

```
state[row][col]  =>  16 byte xếp column-major:
byte[c*4 + r]  <-->  state[r][c]
```

Mỗi lần mã hóa gồm:
1. `AddRoundKey` (vòng 0)
2. 9 vòng chính: `SubBytes → ShiftRows → MixColumns → AddRoundKey`
3. Vòng cuối: `SubBytes → ShiftRows → AddRoundKey` (bỏ `MixColumns`)

---

## 2. Trường Galois GF(2⁸)  *(file: `GaloisField.java`)*

AES dùng số học trên **GF(2⁸)** – tập hợp các đa thức bậc ≤7 có hệ số trong {0,1}, với phép cộng là XOR và phép nhân modulo đa thức bất khả quy:

```
m(x) = x⁸ + x⁴ + x³ + x + 1  =>  0x11b
```

### 2.1 Cộng trong GF(2⁸)

```
a ⊕ b  (XOR từng bit)
```

### 2.2 Nhân với 2 trong GF(2⁸)  *(xtime)*

```
xtime(a) =
  if bit 7 của a = 0:   a << 1
  if bit 7 của a = 1:  (a << 1) XOR 0x1b
```

Lý do: khi dịch trái, nếu bậc 8 xuất hiện thì phải trừ (= XOR trong GF(2)) cho m(x).

### 2.3 Nhân tổng quát – Russian Peasant Multiplication

```java
multiply(a, b):
    result = 0
    while a > 0:
        if a & 1 == 1:  result ^= b        // cộng b vào kết quả
        b = xtime(b)                        // nhân đôi b
        a >>= 1                             // chia đôi a
    return result
```

Ví dụ: `multiply(3, b) = multiply(2, b) XOR multiply(1, b) = xtime(b) XOR b`

---

## 3. S-Box và Inverse S-Box  *(file: `AESTables.java`)*

S-Box là bảng thay thế phi tuyến 256 phần tử, xây dựng qua 2 bước:

### 3.1 Tính nghịch đảo trong GF(2⁸)

```
a⁻¹ = a^(2⁸ - 2) = a^254   (theo định lý Fermat nhỏ trên trường hữu hạn)
```
Quy ước: `0⁻¹ = 0`

### 3.2 Biến đổi affine

```
s = Affine(a⁻¹):

| s₀ |   | 1 0 0 0 1 1 1 1 |   | b₀ |   | 1 |
| s₁ |   | 1 1 0 0 0 1 1 1 |   | b₁ |   | 1 |
| s₂ |   | 1 1 1 0 0 0 1 1 | × | b₂ | ⊕ | 0 |
| s₃ |   | 1 1 1 1 0 0 0 1 |   | b₃ |   | 0 |
| s₄ |   | 1 1 1 1 1 0 0 0 |   | b₄ |   | 0 |
| s₅ |   | 0 1 1 1 1 1 0 0 |   | b₅ |   | 1 |
| s₆ |   | 0 0 1 1 1 1 1 0 |   | b₆ |   | 1 |
| s₇ |   | 0 0 0 1 1 1 1 1 |   | b₇ |   | 0 |
```

Trong code, bảng được **pre-computed** thành mảng `SBOX[256]` và `INV_SBOX[256]` để tra cứu O(1).

---

## 4. Key Expansion  *(file: `AESKeyExpansion.java`)*

Từ 16-byte key gốc, tạo ra **11 round keys** (mỗi key 16 byte):

```
Cho roundKey[i]:
    temp = Word cuối (byte 12–15) của roundKey[i-1]

    RotWord(temp):   [a,b,c,d] → [b,c,d,a]          // xoay trái 1 byte

    SubWord(temp):   thay từng byte qua SBOX

    temp[0] ^= Rcon[i]                               // XOR hằng số vòng

    roundKey[i][0..3]  = roundKey[i-1][0..3]  XOR temp
    roundKey[i][4..7]  = roundKey[i-1][4..7]  XOR roundKey[i][0..3]
    roundKey[i][8..11] = roundKey[i-1][8..11] XOR roundKey[i][4..7]
    roundKey[i][12..15]= roundKey[i-1][12..15] XOR roundKey[i][8..11]
```

### Round Constant Rcon[i]

```
Rcon[i] = [x^(i-1) mod m(x),  0, 0, 0]   (chỉ dùng byte đầu)

Rcon[1] = 0x01,  Rcon[2] = 0x02,  Rcon[3] = 0x04,
Rcon[4] = 0x08,  Rcon[5] = 0x10,  Rcon[6] = 0x20,
Rcon[7] = 0x40,  Rcon[8] = 0x80,  Rcon[9] = 0x1b,  Rcon[10] = 0x36
```

---

## 5. SubBytes / InvSubBytes  *(file: `AESTransformations.java`)*

```
Mã hóa:   state[r][c] = SBOX[ state[r][c] ]
Giải mã:  state[r][c] = INV_SBOX[ state[r][c] ]
```

---

## 6. ShiftRows / InvShiftRows  *(file: `AESTransformations.java`)*

Dịch vòng các hàng:

```
      Mã hóa (shift trái)          Giải mã (shift phải)
Hàng 0: không dịch                 không dịch
Hàng 1: ← 1 byte                   → 1 byte
Hàng 2: ← 2 byte                   → 2 byte
Hàng 3: ← 3 byte                   → 3 byte
```

---

## 7. MixColumns / InvMixColumns  *(file: `AESTransformations.java`)*

Mỗi cột `[a, b, c, d]ᵀ` được nhân với ma trận trong GF(2⁸):

### Mã hóa

```
| s'₀ |   | 02 03 01 01 |   | s₀ |
| s'₁ | = | 01 02 03 01 | × | s₁ |   (nhân trong GF(2⁸))
| s'₂ |   | 01 01 02 03 |   | s₂ |
| s'₃ |   | 03 01 01 02 |   | s₃ |
```

Khai triển (ví dụ hàng đầu):
```
s'₀ = mul(2,s₀) ⊕ mul(3,s₁) ⊕ s₂ ⊕ s₃
```

### Giải mã

```
| s'₀ |   | 0E 0B 0D 09 |   | s₀ |
| s'₁ | = | 09 0E 0B 0D | × | s₁ |
| s'₂ |   | 0D 09 0E 0B |   | s₂ |
| s'₃ |   | 0B 0D 09 0E |   | s₃ |
```

Khai triển:
```
s'₀ = mul(14,s₀) ⊕ mul(11,s₁) ⊕ mul(13,s₂) ⊕ mul(9,s₃)
```

---

## 8. AddRoundKey  *(file: `AESTransformations.java`)*

```
state[r][c]  ^=  roundKey[c*4 + r]
```

Là phép XOR đơn giản, giống nhau cả mã hóa lẫn giải mã (tự nghịch đảo).

---

## 9. Quy trình đầy đủ

### Mã hóa
```
AddRoundKey(state, roundKey[0])
for i = 1 to 9:
    SubBytes(state)
    ShiftRows(state)
    MixColumns(state)
    AddRoundKey(state, roundKey[i])
SubBytes(state)
ShiftRows(state)
AddRoundKey(state, roundKey[10])
```

### Giải mã
```
AddRoundKey(state, roundKey[10])
InvShiftRows(state)
InvSubBytes(state)
for i = 9 downto 1:
    AddRoundKey(state, roundKey[i])
    InvMixColumns(state)
    InvShiftRows(state)
    InvSubBytes(state)
AddRoundKey(state, roundKey[0])
```

---

## 10. Test vector (NIST FIPS 197 – Appendix B)

| |  Giá trị (hex) |
|---|---|
| **Key**         | `000102030405060708090a0b0c0d0e0f` |
| **Plaintext**   | `00112233445566778899aabbccddeeff` |
| **Ciphertext**  | `69c4e0d86a7b0430d8cdb78070b4c55a` |

Chạy `AES.main()` để kiểm chứng.

---

## 11. Tại sao kết quả khác với tool online?

Nếu bồ copy Key và Plaintext vào các trang web mã hóa mà ra kết quả khác, đó thường là do:

1. **Chế độ mã hóa (Mode):** Code hiện tại là AES Core, tương đương mode **ECB**. Các tool online thường mặc định dùng **CBC** (cần IV).
2. **Padding:** AES cần block 16 bytes. Tool online thường mặc định dùng **PKCS7 padding** nên kết quả sẽ dài hơn (thường là 32 bytes).
3. **Định dạng đầu vào (Encoding):** Code dùng **Hex (Raw bytes)**. Tool online thường dùng **String (UTF-8)**. Chuỗi "01" (2 byte ASCII) khác với số 0x01 (1 byte hex).

**Cách kiểm chứng khớp 100%:** 
Vào [CyberChef](https://gchq.github.io/CyberChef/), chọn AES Encrypt với cấu hình:
- **Mode:** ECB
- **Key/Input:** Hex
- **Padding:** None
- **Key:** `000102030405060708090a0b0c0d0e0f`
- **Input:** `00112233445566778899aabbccddeeff`
- **Kết quả sẽ là:** `69c4e0d86a7b0430d8cdb78070b4c55a`
