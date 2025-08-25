-- PostgreSQL Migration Script to add bankName and accountName fields to events table

-- Add bankName column
ALTER TABLE events ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255);

-- Add accountName column  
ALTER TABLE events ADD COLUMN IF NOT EXISTS account_name VARCHAR(255);

-- Update existing records with default values based on branch
UPDATE events SET 
    bank_name = CASE 
        WHEN branch = 'NCCU' THEN '(822) Cathay United Bank'
        WHEN branch = 'NTU' THEN '(700) China Post'
        WHEN branch = 'TAIPEI' THEN '(812) Taiwan Cooperative Bank'
        ELSE '(822) Cathay United Bank'
    END,
    account_name = CASE 
        WHEN branch = 'NCCU' THEN 'SLAM NCCU'
        WHEN branch = 'NTU' THEN 'SLAM NTU'
        WHEN branch = 'TAIPEI' THEN 'SLAM TAIPEI'
        ELSE 'SLAM NCCU'
    END
WHERE bank_name IS NULL OR account_name IS NULL;
