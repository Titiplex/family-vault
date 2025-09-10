PRAGMA foreign_keys= ON;

CREATE TABLE IF NOT EXISTS budget_share_targets
(
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id             INTEGER NOT NULL, -- propriétaire (profil)
    participant_user_id INTEGER NOT NULL, -- personne faisant partie du foyer
    share_percent       REAL    NOT NULL, -- ex: 60.0 (=60%)
    uuid                TEXT,
    updated_at          TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted             INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_share_targets_unq ON budget_share_targets (user_id, participant_user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_share_targets_uuid ON budget_share_targets (uuid);
CREATE TRIGGER IF NOT EXISTS trg_share_targets_ai
    AFTER INSERT
    ON budget_share_targets
BEGIN
    UPDATE budget_share_targets SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_share_targets_au
    AFTER UPDATE
    ON budget_share_targets
BEGIN
    UPDATE budget_share_targets SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* Oplog pour autosync */
CREATE TRIGGER IF NOT EXISTS op_share_targets_ai
    AFTER INSERT
    ON budget_share_targets
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_share_targets', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_share_targets_au
    AFTER UPDATE
    ON budget_share_targets
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_share_targets', NEW.uuid);
END;
