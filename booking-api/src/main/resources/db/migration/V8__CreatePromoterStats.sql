-- Create table for promoter statistics
CREATE TABLE IF NOT EXISTS promoter_stats (
    id SERIAL PRIMARY KEY,
    promoter_id BIGINT NOT NULL UNIQUE,
    visits INT NOT NULL DEFAULT 0,
    total_deposit DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);
