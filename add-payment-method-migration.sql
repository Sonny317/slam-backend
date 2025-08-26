-- membership_applications 테이블에 payment_method 컬럼 추가 (이미 존재하는 경우 무시)
-- MySQL의 경우
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'membership_applications' 
     AND COLUMN_NAME = 'payment_method') = 0,
    'ALTER TABLE membership_applications ADD COLUMN payment_method VARCHAR(255) NOT NULL DEFAULT "unknown"',
    'SELECT "Column payment_method already exists" as message'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- PostgreSQL의 경우 (위의 MySQL 스크립트가 작동하지 않는 경우)
-- DO $$
-- BEGIN
--     IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
--                    WHERE table_name = 'membership_applications' 
--                    AND column_name = 'payment_method') THEN
--         ALTER TABLE membership_applications ADD COLUMN payment_method VARCHAR(255) NOT NULL DEFAULT 'unknown';
--     END IF;
-- END $$;
