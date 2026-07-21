ALTER TABLE knowledge_chunk_embedding
    ADD COLUMN IF NOT EXISTS search_vector TSVECTOR
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(source_name, '')), 'A') ||
        setweight(to_tsvector('simple', coalesce(content, '')), 'B')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_search_vector
    ON knowledge_chunk_embedding USING GIN (search_vector);
