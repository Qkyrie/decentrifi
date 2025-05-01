-- Create raw_invocations table for storing contract invocation data
CREATE TABLE IF NOT EXISTS raw_invocations (
    id SERIAL PRIMARY KEY,
    network VARCHAR(64) NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    block_number BIGINT NOT NULL,
    block_timestamp TIMESTAMP NOT NULL,
    tx_hash VARCHAR(66) NOT NULL,
    from_address VARCHAR(42) NOT NULL,
    function_selector VARCHAR(10) NOT NULL,
    function_name VARCHAR(64),
    input_args JSONB NOT NULL,
    status BOOLEAN NOT NULL,
    gas_used BIGINT NOT NULL
);

-- Create indexes for optimizing queries
CREATE INDEX IF NOT EXISTS idx_raw_invocations_block_number ON raw_invocations(block_number);
CREATE INDEX IF NOT EXISTS idx_raw_invocations_tx_hash ON raw_invocations(tx_hash);
CREATE INDEX IF NOT EXISTS idx_raw_invocations_contract_address ON raw_invocations(contract_address);
CREATE INDEX IF NOT EXISTS idx_raw_invocations_function_selector ON raw_invocations(function_selector);
CREATE INDEX IF NOT EXISTS idx_raw_invocations_from_address ON raw_invocations(from_address);
CREATE INDEX IF NOT EXISTS idx_raw_invocations_block_timestamp ON raw_invocations(block_timestamp);

-- Create ingestion_metadata table for tracking ingestion state
CREATE TABLE IF NOT EXISTS ingestion_metadata (
    key VARCHAR(64) PRIMARY KEY,
    value VARCHAR(64) NOT NULL
);

-- Initialize ingestion_metadata with last_processed_block
INSERT INTO ingestion_metadata (key, value) 
VALUES ('last_processed_block', '0')
ON CONFLICT (key) DO NOTHING;

-- Create time-series hypertable for raw_invocations using TimescaleDB
-- This allows more efficient time-based queries and retention policies
SELECT create_hypertable('raw_invocations', 'block_timestamp', 
                          chunk_time_interval => INTERVAL '1 day',
                          if_not_exists => TRUE);
