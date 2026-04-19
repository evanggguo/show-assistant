-- V5__add_owner_credentials.sql
-- Add login credential columns to owners table for multi-owner account management

ALTER TABLE owners
    ADD COLUMN IF NOT EXISTS username      VARCHAR(50),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_owners_username ON owners (username)
    WHERE username IS NOT NULL;
