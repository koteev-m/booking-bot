ALTER TABLE guest_list
    ADD COLUMN IF NOT EXISTS checked_in BOOLEAN DEFAULT FALSE;
CREATE INDEX IF NOT EXISTS idx_guest_list_checked_in ON guest_list(checked_in);

