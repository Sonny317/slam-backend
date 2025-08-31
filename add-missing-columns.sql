-- Users 테이블에 OAuth 관련 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS oauth_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS provider_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url VARCHAR(500);

-- Events 테이블에 Early Bird 관련 컬럼 추가 (이미 있을 수도 있음)
ALTER TABLE events ADD COLUMN IF NOT EXISTS early_bird_end_date DATETIME;
ALTER TABLE events ADD COLUMN IF NOT EXISTS early_bird_price INT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS early_bird_capacity INT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS capacity_warning_threshold INT;
ALTER TABLE events ADD COLUMN IF NOT EXISTS show_capacity_warning BOOLEAN DEFAULT FALSE;
ALTER TABLE events ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);
ALTER TABLE events ADD COLUMN IF NOT EXISTS account_name VARCHAR(255);

-- 기존 데이터에 기본값 설정
UPDATE users SET oauth_id = NULL WHERE oauth_id IS NULL;
UPDATE users SET provider = 'local' WHERE provider IS NULL;
UPDATE users SET provider_id = NULL WHERE provider_id IS NULL;
UPDATE users SET profile_image_url = NULL WHERE profile_image_url IS NULL;

