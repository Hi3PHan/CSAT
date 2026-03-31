-- data.sql — Seed data cho hệ thống CSAT-BMTT
-- Password "password123" hashed by CustomSHA256PasswordEncoder
-- Format: {salt-base64}${sha256Hex(salt + rawPassword)}

-- ─── Tài khoản ───────────────────────────────────────────────────────────────
-- Level 1: Nhân viên (nhanvien / password123)
-- Level 2: Trưởng phòng (truongphong / password123)
-- Level 3: Giám đốc (giamdoc / password123)

-- Dọn dữ liệu động trước khi seed
TRUNCATE TABLE sessions;
TRUNCATE TABLE user_action_logs;

INSERT INTO accounts (username, password_hash, level) VALUES
('nhanvien',   'MDEyMzQ1Njc4OWFiY2RlZg==$78459b0a33fdceb4f999bfa11cdb346946f911530ad15ebe53ec8da8058ec122', 1),
('truongphong','ZmVkY2JhOTg3NjU0MzIxMA==$b321e6c12d8dc3409d33083a537e46d92fcb170ab1b4bb63a53369d9014223ae', 2),
('giamdoc',    'QUJDREVGR0hJSktMTU5PUA==$f25cac2709817c03e46668ee398315855f74a40f7d7e3973c94dccca101069eb', 3)
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), level = VALUES(level);

-- Khong seed du lieu users o day.
