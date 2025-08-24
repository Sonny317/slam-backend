-- ===================================================================
-- 기존 이벤트들의 EventType과 ProductType 자동 수정 (PostgreSQL용)
-- ===================================================================

-- Regular SLAM Meet 이벤트들 설정
UPDATE events 
SET event_type = 'REGULAR_MEET', 
    product_type = 'Membership'
WHERE LOWER(theme) LIKE '%regular%' 
  AND LOWER(theme) LIKE '%slam%' 
  AND LOWER(theme) LIKE '%meet%';

-- 그 외 모든 이벤트들을 Special Event로 설정
UPDATE events 
SET event_type = 'SPECIAL_EVENT', 
    product_type = 'Ticket'
WHERE NOT (LOWER(theme) LIKE '%regular%' 
           AND LOWER(theme) LIKE '%slam%' 
           AND LOWER(theme) LIKE '%meet%')
   OR theme IS NULL;

-- outing 테마 이벤트들 확실히 Special Event로 설정
UPDATE events 
SET event_type = 'SPECIAL_EVENT', 
    product_type = 'Ticket'
WHERE LOWER(theme) LIKE '%outing%';

-- 확인용 쿼리
SELECT title, theme, event_type, product_type 
FROM events 
ORDER BY id;
