-- V2__seed_owner.sql
-- TDD 5.3 — Seed initial demo Owner data

INSERT INTO owners (name, tagline, avatar_url, contact, config)
VALUES (
    'Demo User',
    'Full-stack Developer & Indie Maker',
    NULL,
    '{"email": "demo@dossier.app", "github": "https://github.com/demo"}',
    '{"theme": "default", "language": "en"}'
)
ON CONFLICT DO NOTHING;
