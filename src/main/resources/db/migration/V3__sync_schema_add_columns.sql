PRAGMA foreign_keys= ON;

/* USERS */
ALTER TABLE users
    ADD COLUMN uuid TEXT;
ALTER TABLE users
    ADD COLUMN updated_at TEXT;
ALTER TABLE users
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* LISTS */
ALTER TABLE lists
    ADD COLUMN uuid TEXT;
ALTER TABLE lists
    ADD COLUMN updated_at TEXT;
ALTER TABLE lists
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* EVENTS */
ALTER TABLE events
    ADD COLUMN uuid TEXT;
ALTER TABLE events
    ADD COLUMN updated_at TEXT;
ALTER TABLE events
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* RECIPES */
ALTER TABLE recipes
    ADD COLUMN uuid TEXT;
ALTER TABLE recipes
    ADD COLUMN updated_at TEXT;
ALTER TABLE recipes
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* MESSAGES */
ALTER TABLE messages
    ADD COLUMN uuid TEXT;
ALTER TABLE messages
    ADD COLUMN updated_at TEXT;
ALTER TABLE messages
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* PHOTOS */
ALTER TABLE photos
    ADD COLUMN uuid TEXT;
ALTER TABLE photos
    ADD COLUMN updated_at TEXT;
ALTER TABLE photos
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* CONTACTS */
ALTER TABLE contacts
    ADD COLUMN uuid TEXT;
ALTER TABLE contacts
    ADD COLUMN updated_at TEXT;
ALTER TABLE contacts
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* ACTIVITIES */
ALTER TABLE activities
    ADD COLUMN uuid TEXT;
ALTER TABLE activities
    ADD COLUMN updated_at TEXT;
ALTER TABLE activities
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* DOCUMENTS */
ALTER TABLE documents
    ADD COLUMN uuid TEXT;
ALTER TABLE documents
    ADD COLUMN updated_at TEXT;
ALTER TABLE documents
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* BUDGET */
ALTER TABLE budget
    ADD COLUMN uuid TEXT;
ALTER TABLE budget
    ADD COLUMN updated_at TEXT;
ALTER TABLE budget
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* MEALS */
ALTER TABLE meals
    ADD COLUMN uuid TEXT;
ALTER TABLE meals
    ADD COLUMN updated_at TEXT;
ALTER TABLE meals
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* TIMETABLE */
ALTER TABLE timetable
    ADD COLUMN uuid TEXT;
ALTER TABLE timetable
    ADD COLUMN updated_at TEXT;
ALTER TABLE timetable
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* PLACES */
ALTER TABLE places
    ADD COLUMN uuid TEXT;
ALTER TABLE places
    ADD COLUMN updated_at TEXT;
ALTER TABLE places
    ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;

/* P2P tables (créations simples = OK ici) */
CREATE TABLE IF NOT EXISTS trusted_peers
(
    device_id           TEXT PRIMARY KEY,
    public_key          TEXT NOT NULL,
    address             TEXT,
    port                INTEGER,
    pairing_secret_hash TEXT NOT NULL,
    added_at            TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_peer_state
(
    peer_device_id TEXT PRIMARY KEY,
    last_sync_at   TEXT NOT NULL
);
