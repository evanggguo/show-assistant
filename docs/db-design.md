# Dossier - Database Schema UML

```mermaid
erDiagram

    owners {
        bigserial id PK
        varchar(100) name "NOT NULL"
        varchar(255) tagline
        varchar(500) avatar_url
        jsonb contact "default {}"
        jsonb config "default {}"
        timestamptz created_at
    }

    client_users {
        bigserial id PK
        varchar(50) sso_provider "NOT NULL google/github"
        varchar(255) sso_id "NOT NULL"
        varchar(100) nickname
        varchar(500) avatar_url
        timestamptz created_at
    }

    conversations {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        bigint user_id FK "NULL = guest"
        varchar(50) source "default web"
        timestamptz created_at
        timestamptz updated_at
    }

    messages {
        bigserial id PK
        bigint conversation_id FK "NOT NULL"
        varchar(20) role "user / assistant / system"
        text content "NOT NULL"
        timestamptz created_at
    }

    dynamic_suggestions {
        bigserial id PK
        bigint message_id FK "NOT NULL"
        varchar(500) text "NOT NULL"
        int sort_order "default 0"
    }

    prompt_suggestions {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(500) text "NOT NULL"
        int sort_order "default 0"
        boolean enabled "default true"
    }

    knowledge_entries {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(50) type "skill/experience/project/education/service/other"
        varchar(255) title
        text content "NOT NULL"
        vector_1536 embedding "Phase 3 vector retrieval"
        bigint source_doc FK "nullable"
        timestamptz created_at
    }

    documents {
        bigserial id PK
        bigint owner_id FK "NOT NULL"
        varchar(255) filename "NOT NULL"
        varchar(50) file_type
        bigint file_size
        varchar(500) file_path
        varchar(50) status "pending/processing/done/failed"
        timestamptz created_at
    }

    owners ||--o{ conversations : "has"
    owners ||--o{ prompt_suggestions : "configures"
    owners ||--o{ knowledge_entries : "maintains"
    owners ||--o{ documents : "uploads"

    client_users ||--o{ conversations : "initiates"

    conversations ||--o{ messages : "contains"

    messages ||--o{ dynamic_suggestions : "generates"

    documents ||--o{ knowledge_entries : "extracted from"
```

---

## Table Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| owners → conversations | 1 : N | One owner has multiple conversation sessions |
| owners → prompt_suggestions | 1 : N | Owner configures initial home-screen suggestions in the admin console |
| owners → knowledge_entries | 1 : N | Knowledge base entries maintained by the owner |
| owners → documents | 1 : N | Files uploaded by the owner |
| client_users → conversations | 1 : N | Conversations initiated by logged-in users (guest: user_id = NULL) |
| conversations → messages | 1 : N | A conversation contains multiple messages |
| messages → dynamic_suggestions | 1 : N | Each assistant message generates 2–4 dynamic follow-up suggestions |
| documents → knowledge_entries | 1 : N | One file can yield multiple knowledge entries (source_doc nullable = manually entered) |

---

## Key Indexes

| Index Name | Table | Column(s) | Type | Purpose |
|------------|-------|-----------|------|---------|
| `idx_knowledge_entries_embedding` | knowledge_entries | embedding | HNSW (cosine) | Phase 3 vector similarity search |
| `idx_messages_conversation_id` | messages | conversation_id, created_at | B-tree | Load message history by conversation |
| `idx_knowledge_entries_owner_id` | knowledge_entries | owner_id | B-tree | Filter knowledge entries by owner |
| `idx_prompt_suggestions_owner_id` | prompt_suggestions | owner_id, sort_order | B-tree | Sorted query for home-screen suggestions |
| `idx_conversations_owner_id` | conversations | owner_id | B-tree | Admin console conversation list |
| UNIQUE (sso_provider, sso_id) | client_users | — | Unique | Prevent duplicate SSO user registration |
