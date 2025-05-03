-- Create contracts table to store contract ABIs, addresses, and chain information
CREATE TABLE contracts
(
    id         SERIAL PRIMARY KEY,
    address    VARCHAR(42) NOT NULL,       -- The contract address (e.g. '0x6B17…')
    abi        TEXT NOT NULL,              -- The contract ABI as JSON string
    chain      VARCHAR(64) NOT NULL,       -- The blockchain network (e.g. 'ethereum-mainnet', 'bsc-mainnet')
    name       VARCHAR(128),               -- Optional name for the contract
    created_at TIMESTAMPTZ NOT NULL,       -- When the record was created
    updated_at TIMESTAMPTZ NOT NULL        -- When the record was last updated
);

-- Create index on address for faster lookups
CREATE INDEX idx_contracts_address ON contracts(address);

-- Create index on chain for faster lookups
CREATE INDEX idx_contracts_chain ON contracts(chain);

-- Create unique index on address and chain combination
CREATE UNIQUE INDEX idx_contracts_address_chain ON contracts(address, chain);

-- Add comment to describe the table
COMMENT ON TABLE contracts IS 'Stores information about blockchain smart contracts including their ABI and address';
