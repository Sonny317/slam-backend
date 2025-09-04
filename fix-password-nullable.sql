-- Google OAuth 사용자를 위해 password 컬럼을 nullable로 변경
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 기존에 빈 문자열로 저장된 password를 null로 변경 (Google OAuth 사용자)
UPDATE users SET password = NULL WHERE password = '' AND provider = 'google';

-- 변경사항 확인
SELECT email, password, provider FROM users WHERE provider = 'google';
