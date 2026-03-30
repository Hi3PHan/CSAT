-- ─────────────────────────────────────────────────────────────
-- schema.sql — Tạo bảng cho hệ thống CSAT-BMTT
-- ─────────────────────────────────────────────────────────────

-- Bảng accounts: Quản lý tài khoản đăng nhập và phân quyền
CREATE TABLE IF NOT EXISTS accounts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,         -- salted SHA-256 hash, khong phai AES
    level         TINYINT NOT NULL DEFAULT 1,    -- 1: Nhân viên, 2: Trưởng phòng, 3: Giám đốc
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Bảng users: Lưu thông tin nhạy cảm của nhân viên (mã hóa AES Data-at-Rest)
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name    TEXT NOT NULL,     -- AES encrypted
    cccd         TEXT NOT NULL,     -- AES encrypted
    phone        TEXT NOT NULL,     -- AES encrypted
    email        TEXT NOT NULL,     -- AES encrypted
    bank_account TEXT NOT NULL,     -- AES encrypted
    salary       TEXT NOT NULL,     -- AES encrypted (lưu dưới dạng String)
    algo         VARCHAR(50) NOT NULL DEFAULT 'AES-128-ECB',  -- Thuật toán đã dùng
    account_id   BIGINT,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL
);

-- Bảng sessions: Quản lý phiên đăng nhập (Challenge-Response)
CREATE TABLE IF NOT EXISTS sessions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(512) NOT NULL UNIQUE,
    token_type  ENUM('challenge', 'access') NOT NULL,
    user_id     BIGINT,                            -- NULL khi là challenge token
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES accounts(id) ON DELETE CASCADE
);
