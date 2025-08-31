-- Add oauth_id column to users table
ALTER TABLE users ADD COLUMN oauth_id VARCHAR(255);

-- Add provider column if it doesn't exist
ALTER TABLE users ADD COLUMN provider VARCHAR(50);

-- Add provider_id column if it doesn't exist  
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- Add profile_image_url column if it doesn't exist
ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500);

-- Update existing records to have default values
UPDATE users SET oauth_id = NULL WHERE oauth_id IS NULL;
UPDATE users SET provider = 'local' WHERE provider IS NULL;
UPDATE users SET provider_id = NULL WHERE provider_id IS NULL;

