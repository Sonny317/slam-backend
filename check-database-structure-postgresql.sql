-- PostgreSQL 데이터베이스 구조 확인 스크립트
-- 이 스크립트는 마이그레이션 후 데이터베이스 구조를 확인하는 용도입니다.

-- 1. events 테이블 구조 확인
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'events' 
ORDER BY ordinal_position;

-- 2. membership_applications 테이블 구조 확인
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'membership_applications' 
ORDER BY ordinal_position;

-- 3. users 테이블 구조 확인
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'users' 
ORDER BY ordinal_position;

-- 4. user_memberships 테이블 구조 확인
SELECT 
    column_name, 
    data_type, 
    is_nullable, 
    column_default,
    character_maximum_length
FROM information_schema.columns 
WHERE table_name = 'user_memberships' 
ORDER BY ordinal_position;

-- 5. 이벤트 시퀀스 확인
SELECT 
    id, 
    title, 
    branch, 
    event_type, 
    event_sequence,
    created_at
FROM events 
WHERE event_type = 'REGULAR_MEET' 
ORDER BY branch, event_sequence;

-- 6. 활성 멤버십 확인
SELECT 
    um.id,
    u.email,
    um.branch_name,
    um.status,
    um.id as membership_id
FROM user_memberships um
JOIN users u ON um.user_id = u.id
WHERE um.status = 'ACTIVE'
ORDER BY um.branch_name, u.email;

-- 7. 관리자 계정 확인
SELECT 
    id, 
    name, 
    email, 
    role, 
    status,
    created_at
FROM users 
WHERE role IN ('ADMIN', 'STAFF', 'PRESIDENT', 'LEADER')
ORDER BY role, email;
