-- ===================================================================
-- 안전한 Event/User 테이블 업데이트 (PostgreSQL용)
-- ===================================================================

-- Event 테이블에 새로운 컬럼들 추가
ALTER TABLE events ADD COLUMN IF NOT EXISTS bank_account VARCHAR(100);
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_type VARCHAR(20) DEFAULT 'REGULAR_MEET';
ALTER TABLE events ADD COLUMN IF NOT EXISTS event_sequence INTEGER;
ALTER TABLE events ADD COLUMN IF NOT EXISTS product_type VARCHAR(20) DEFAULT 'Membership';

-- User 테이블에 membership_type 컬럼 추가
ALTER TABLE users ADD COLUMN IF NOT EXISTS membership_type VARCHAR(20) DEFAULT 'NONE';

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
