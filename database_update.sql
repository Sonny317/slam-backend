-- MembershipApplication 테이블에 event_id 컬럼 추가 (PostgreSQL)
ALTER TABLE membership_applications ADD COLUMN event_id BIGINT;

-- MembershipApplication 테이블에 event_id 컬럼 추가 (MySQL)
-- ALTER TABLE membership_applications ADD COLUMN event_id BIGINT;
