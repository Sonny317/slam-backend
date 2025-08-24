-- ===================================================================
-- Event 테이블에 새로운 필드 추가
-- Early bird 가격, 데드라인, 인원수 마감, 계좌 번호 등
-- ===================================================================

-- MySQL용 Event 테이블 업데이트
ALTER TABLE events ADD COLUMN bank_account VARCHAR(100) COMMENT '계좌 번호';

-- 이미 존재하는 컬럼들 확인 (오류 발생 시 무시)
-- ALTER TABLE events ADD COLUMN early_bird_price INT COMMENT '얼리버드 가격';
-- ALTER TABLE events ADD COLUMN early_bird_end_date DATETIME COMMENT '얼리버드 기간 종료일';
-- ALTER TABLE events ADD COLUMN early_bird_capacity INT COMMENT '얼리버드 인원수 제한';
-- ALTER TABLE events ADD COLUMN registration_deadline DATETIME COMMENT '등록 마감일';
-- ALTER TABLE events ADD COLUMN capacity_warning_threshold INT COMMENT '용량 경고 임계값';
-- ALTER TABLE events ADD COLUMN show_capacity_warning BOOLEAN COMMENT '용량 경고 표시 여부';
-- ALTER TABLE events ADD COLUMN end_time VARCHAR(20) COMMENT '종료 시간';

-- PostgreSQL용 Event 테이블 업데이트
DO $$ 
BEGIN 
    -- bank_account 컬럼 추가
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='bank_account') THEN
        ALTER TABLE events ADD COLUMN bank_account VARCHAR(100);
    END IF;
    
    -- 기존 컬럼들은 이미 존재할 가능성이 높으므로 체크만 함
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='early_bird_price') THEN
        ALTER TABLE events ADD COLUMN early_bird_price INTEGER;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='early_bird_end_date') THEN
        ALTER TABLE events ADD COLUMN early_bird_end_date TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='early_bird_capacity') THEN
        ALTER TABLE events ADD COLUMN early_bird_capacity INTEGER;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='registration_deadline') THEN
        ALTER TABLE events ADD COLUMN registration_deadline TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='capacity_warning_threshold') THEN
        ALTER TABLE events ADD COLUMN capacity_warning_threshold INTEGER;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='show_capacity_warning') THEN
        ALTER TABLE events ADD COLUMN show_capacity_warning BOOLEAN;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='events' AND column_name='end_time') THEN
        ALTER TABLE events ADD COLUMN end_time VARCHAR(20);
    END IF;
END $$;

-- 인덱스 추가 (성능 최적화)
CREATE INDEX IF NOT EXISTS idx_events_early_bird_end_date ON events(early_bird_end_date);
CREATE INDEX IF NOT EXISTS idx_events_registration_deadline ON events(registration_deadline);

SELECT 'Event migration completed successfully!' as Result;
