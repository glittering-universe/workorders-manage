-- 修复用户密码哈希
-- 所有用户的密码都是 "password123"，但需要为每个用户生成正确的BCrypt哈希

-- 更新用户密码哈希（password123的正确哈希）
UPDATE users SET password = '$2a$10$hA1pFGerbScLkTPdP7DPvuHMN3nX.nzf3dwemT22mGYap7BNnj/Ba' WHERE username = 'manager1';
UPDATE users SET password = '$2a$10$hA1pFGerbScLkTPdP7DPvuHMN3nX.nzf3dwemT22mGYap7BNnj/Ba' WHERE username = 'operator1';
UPDATE users SET password = '$2a$10$hA1pFGerbScLkTPdP7DPvuHMN3nX.nzf3dwemT22mGYap7BNnj/Ba' WHERE username = 'user1';

-- admin用户已经有正确的密码哈希，无需更新
