-- Add chunk_storage_id column to file_chunks table.
-- This is the UUID used as the filename on the storage node disk: {chunk_storage_id}.enc
ALTER TABLE file_chunks ADD COLUMN IF NOT EXISTS chunk_storage_id UUID;

-- Back-fill existing rows with a random UUID so NOT NULL constraint can be applied
UPDATE file_chunks SET chunk_storage_id = gen_random_uuid() WHERE chunk_storage_id IS NULL;

ALTER TABLE file_chunks ALTER COLUMN chunk_storage_id SET NOT NULL;
