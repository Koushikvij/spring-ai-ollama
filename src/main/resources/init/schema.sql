CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

CREATE TABLE IF NOT EXISTS vector_store (
                                            id UUID PRIMARY KEY, -- id should be TEXT (not UUID type)
                                            content TEXT NOT NULL,
                                            metadata JSONB,
                                            embedding VECTOR(1024) NOT NULL
    );

-- Create HNSW index for fast search
CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);