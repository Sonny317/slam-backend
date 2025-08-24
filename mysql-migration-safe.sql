-- ===================================================================
-- MySQL 안전 마이그레이션 스크립트
-- 이미 존재하는 컬럼은 오류를 무시하고 계속 진행
-- ===================================================================

-- 각 컬럼을 개별적으로 추가 (컬럼이 이미 존재하면 오류 발생하지만 스크립트는 계속 진행)

SET @sql = 'ALTER TABLE users ADD COLUMN user_type VARCHAR(50) COMMENT ''사용자 타입''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'user_type');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''user_type column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN other_major VARCHAR(100) COMMENT ''기타 전공''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'other_major');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''other_major column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN professional_status VARCHAR(100) COMMENT ''직업 상태''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'professional_status');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''professional_status column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN country VARCHAR(50) COMMENT ''국가''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'country');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''country column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN food_allergies VARCHAR(500) COMMENT ''음식 알레르기''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'food_allergies');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''food_allergies column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN payment_method VARCHAR(50) COMMENT ''결제 방법''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'payment_method');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''payment_method column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN bank_last5 VARCHAR(10) COMMENT ''계좌번호 뒤 5자리''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'bank_last5');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''bank_last5 column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN industry VARCHAR(100) COMMENT ''업종''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'industry');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''industry column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN networking_goal VARCHAR(100) COMMENT ''네트워킹 목표''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'networking_goal');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''networking_goal column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = 'ALTER TABLE users ADD COLUMN other_networking_goal VARCHAR(200) COMMENT ''기타 네트워킹 목표''';
SET @column_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'other_networking_goal');
SET @sql = IF(@column_exists = 0, @sql, 'SELECT ''other_networking_goal column already exists'' as Info');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 데이터 마이그레이션: 기존 membership_applications에서 users로 복사
UPDATE users u
JOIN (
    SELECT 
        ma.user_id,
        ma.user_type,
        ma.student_id,
        ma.major,
        ma.other_major,
        ma.professional_status,
        ma.country,
        ma.phone,
        ma.food_allergies,
        ma.payment_method,
        ma.bank_last5,
        ROW_NUMBER() OVER (PARTITION BY ma.user_id ORDER BY ma.created_at DESC) as rn
    FROM membership_applications ma
    WHERE ma.status = 'APPROVED' OR ma.status = 'payment_pending'
) latest_app ON u.id = latest_app.user_id AND latest_app.rn = 1
SET 
    u.user_type = COALESCE(u.user_type, latest_app.user_type),
    u.student_id = COALESCE(u.student_id, latest_app.student_id),
    u.major = COALESCE(u.major, latest_app.major),
    u.other_major = COALESCE(u.other_major, latest_app.other_major),
    u.professional_status = COALESCE(u.professional_status, latest_app.professional_status),
    u.country = COALESCE(u.country, latest_app.country),
    u.nationality = COALESCE(u.nationality, latest_app.country),
    u.phone = COALESCE(u.phone, latest_app.phone),
    u.food_allergies = COALESCE(u.food_allergies, latest_app.food_allergies),
    u.payment_method = COALESCE(u.payment_method, latest_app.payment_method),
    u.bank_last5 = COALESCE(u.bank_last5, latest_app.bank_last5);

-- 인덱스 추가
CREATE INDEX idx_users_user_type ON users(user_type);
CREATE INDEX idx_users_country ON users(country);
CREATE INDEX idx_users_nationality ON users(nationality);
CREATE INDEX idx_users_professional_status ON users(professional_status);

SELECT 'Migration completed successfully!' as Result;
