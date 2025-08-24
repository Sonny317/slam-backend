-- ===================================================================
-- 안전한 Event/User 테이블 업데이트 (중복 컬럼 체크 포함)
-- ===================================================================

-- Event 테이블에 새로운 컬럼들 추가 (IF NOT EXISTS 체크)
-- 1. bank_account 컬럼 체크 및 추가
SET @sql = CONCAT('SELECT COUNT(*) INTO @count FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ''events'' AND column_name = ''bank_account''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@count = 0, 'ALTER TABLE events ADD COLUMN bank_account VARCHAR(100) COMMENT ''계좌 번호''', 'SELECT ''bank_account column already exists'' as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. event_type 컬럼 체크 및 추가
SET @sql = CONCAT('SELECT COUNT(*) INTO @count FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ''events'' AND column_name = ''event_type''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@count = 0, 'ALTER TABLE events ADD COLUMN event_type VARCHAR(20) DEFAULT ''REGULAR_MEET'' COMMENT ''이벤트 타입''', 'SELECT ''event_type column already exists'' as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. event_sequence 컬럼 체크 및 추가
SET @sql = CONCAT('SELECT COUNT(*) INTO @count FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ''events'' AND column_name = ''event_sequence''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@count = 0, 'ALTER TABLE events ADD COLUMN event_sequence INT COMMENT ''이벤트 순서''', 'SELECT ''event_sequence column already exists'' as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. product_type 컬럼 체크 및 추가
SET @sql = CONCAT('SELECT COUNT(*) INTO @count FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ''events'' AND column_name = ''product_type''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@count = 0, 'ALTER TABLE events ADD COLUMN product_type VARCHAR(20) DEFAULT ''Membership'' COMMENT ''상품 타입''', 'SELECT ''product_type column already exists'' as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- User 테이블에 membership_type 컬럼 추가
-- 5. membership_type 컬럼 체크 및 추가
SET @sql = CONCAT('SELECT COUNT(*) INTO @count FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ''users'' AND column_name = ''membership_type''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@count = 0, 'ALTER TABLE users ADD COLUMN membership_type VARCHAR(20) DEFAULT ''NONE'' COMMENT ''멤버십 타입''', 'SELECT ''membership_type column already exists'' as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 기존 이벤트들에 기본값 설정 (NULL인 경우에만)
UPDATE events 
SET event_type = 'REGULAR_MEET' 
WHERE event_type IS NULL;

UPDATE events 
SET product_type = 'Membership' 
WHERE product_type IS NULL;

-- 확인용 쿼리
SELECT 'Migration completed successfully' as status;
SELECT COUNT(*) as total_events FROM events;
SELECT COUNT(*) as events_with_type FROM events WHERE event_type IS NOT NULL;
SELECT COUNT(*) as events_with_product_type FROM events WHERE product_type IS NOT NULL;
