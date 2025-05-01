-- 1. Raw invocations table (keep last 7 days via retention policy)
CREATE TABLE raw_invocations (
                                 id                SERIAL            PRIMARY KEY,
                                 network           TEXT              NOT NULL,      -- e.g. 'ethereum-mainnet'
                                 contract_address  TEXT              NOT NULL,      -- e.g. '0x6B17…'
                                 block_number      BIGINT            NOT NULL,
                                 block_timestamp   TIMESTAMPTZ       NOT NULL,
                                 tx_hash           TEXT              NOT NULL,
                                 from_address      TEXT              NOT NULL,
                                 function_selector CHAR(10)          NOT NULL,      -- first 4 bytes as hex, e.g. '0xa9059cbb'
                                 function_name     TEXT,                            -- decoded by ABI
                                 input_args        JSONB,                          -- arbitrary args, e.g. { "to":"0x…", "value":"1000000" }
                                 status            BOOLEAN           NOT NULL,      -- receipt.status (true=success)
                                 gas_used          BIGINT            NOT NULL
);

-- Convert to a hypertable on block_timestamp
SELECT create_hypertable('raw_invocations', 'block_timestamp', if_not_exists => TRUE);

-- Add 7-day retention
SELECT add_retention_policy('raw_invocations', INTERVAL '7 days');


-- 2. Hourly aggregates
CREATE TABLE metrics_agg (
                             network           TEXT              NOT NULL,
                             contract_address  TEXT              NOT NULL,
                             bucket_start      TIMESTAMPTZ       NOT NULL,      -- e.g. '2025-04-30 14:00:00+00'
                             metric            TEXT              NOT NULL,      -- 'calls', 'errors', 'gas_avg', 'active_wallets'
                             metric_key        TEXT,                            -- grouping key (e.g. function_name for 'calls')
                             value             DOUBLE PRECISION NOT NULL,
                             PRIMARY KEY (network, contract_address, bucket_start, metric, metric_key)
);

-- Convert to hypertable on bucket_start
SELECT create_hypertable('metrics_agg', 'bucket_start', if_not_exists => TRUE);
