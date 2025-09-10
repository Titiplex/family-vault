PRAGMA foreign_keys= ON;

/* USERS */

UPDATE users
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE users
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_uuid ON users (uuid);
CREATE INDEX IF NOT EXISTS idx_users_updated ON users (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_users_ai
    AFTER INSERT
    ON users
BEGIN
    UPDATE users SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_users_au
    AFTER UPDATE
    ON users
BEGIN
    UPDATE users SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* LISTS */

UPDATE lists
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE lists
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_lists_uuid ON lists (uuid);
CREATE INDEX IF NOT EXISTS idx_lists_updated ON lists (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_lists_ai
    AFTER INSERT
    ON lists
BEGIN
    UPDATE lists SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_lists_au
    AFTER UPDATE
    ON lists
BEGIN
    UPDATE lists SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* EVENTS */

UPDATE events
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE events
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_events_uuid ON events (uuid);
CREATE INDEX IF NOT EXISTS idx_events_updated ON events (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_events_ai
    AFTER INSERT
    ON events
BEGIN
    UPDATE events SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_events_au
    AFTER UPDATE
    ON events
BEGIN
    UPDATE events SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* RECIPES */

UPDATE recipes
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE recipes
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_recipes_uuid ON recipes (uuid);
CREATE INDEX IF NOT EXISTS idx_recipes_updated ON recipes (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_recipes_ai
    AFTER INSERT
    ON recipes
BEGIN
    UPDATE recipes SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_recipes_au
    AFTER UPDATE
    ON recipes
BEGIN
    UPDATE recipes SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* MESSAGES */

UPDATE messages
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE messages
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_messages_uuid ON messages (uuid);
CREATE INDEX IF NOT EXISTS idx_messages_updated ON messages (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_messages_ai
    AFTER INSERT
    ON messages
BEGIN
    UPDATE messages SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_messages_au
    AFTER UPDATE
    ON messages
BEGIN
    UPDATE messages SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* PHOTOS */

UPDATE photos
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE photos
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_photos_uuid ON photos (uuid);
CREATE INDEX IF NOT EXISTS idx_photos_updated ON photos (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_photos_ai
    AFTER INSERT
    ON photos
BEGIN
    UPDATE photos SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_photos_au
    AFTER UPDATE
    ON photos
BEGIN
    UPDATE photos SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* CONTACTS */

UPDATE contacts
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE contacts
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_contacts_uuid ON contacts (uuid);
CREATE INDEX IF NOT EXISTS idx_contacts_updated ON contacts (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_contacts_ai
    AFTER INSERT
    ON contacts
BEGIN
    UPDATE contacts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_contacts_au
    AFTER UPDATE
    ON contacts
BEGIN
    UPDATE contacts SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* ACTIVITIES */

UPDATE activities
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE activities
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_activities_uuid ON activities (uuid);
CREATE INDEX IF NOT EXISTS idx_activities_updated ON activities (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_activities_ai
    AFTER INSERT
    ON activities
BEGIN
    UPDATE activities SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_activities_au
    AFTER UPDATE
    ON activities
BEGIN
    UPDATE activities SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* DOCUMENTS */

UPDATE documents
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE documents
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_documents_uuid ON documents (uuid);
CREATE INDEX IF NOT EXISTS idx_documents_updated ON documents (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_documents_ai
    AFTER INSERT
    ON documents
BEGIN
    UPDATE documents SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_documents_au
    AFTER UPDATE
    ON documents
BEGIN
    UPDATE documents SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* BUDGET */

UPDATE budget
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE budget
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_uuid ON budget (uuid);
CREATE INDEX IF NOT EXISTS idx_budget_updated ON budget (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_budget_ai
    AFTER INSERT
    ON budget
BEGIN
    UPDATE budget SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_budget_au
    AFTER UPDATE
    ON budget
BEGIN
    UPDATE budget SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* MEALS */

UPDATE meals
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE meals
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_meals_uuid ON meals (uuid);
CREATE INDEX IF NOT EXISTS idx_meals_updated ON meals (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_meals_ai
    AFTER INSERT
    ON meals
BEGIN
    UPDATE meals SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_meals_au
    AFTER UPDATE
    ON meals
BEGIN
    UPDATE meals SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* TIMETABLE */

UPDATE timetable
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE timetable
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_timetable_uuid ON timetable (uuid);
CREATE INDEX IF NOT EXISTS idx_timetable_updated ON timetable (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_timetable_ai
    AFTER INSERT
    ON timetable
BEGIN
    UPDATE timetable SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_timetable_au
    AFTER UPDATE
    ON timetable
BEGIN
    UPDATE timetable SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* PLACES */

UPDATE places
SET uuid = COALESCE(uuid, lower(hex(randomblob(16))));
UPDATE places
SET updated_at = COALESCE(updated_at, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'));
CREATE UNIQUE INDEX IF NOT EXISTS idx_places_uuid ON places (uuid);
CREATE INDEX IF NOT EXISTS idx_places_updated ON places (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_places_ai
    AFTER INSERT
    ON places
BEGIN
    UPDATE places SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_places_au
    AFTER UPDATE
    ON places
BEGIN
    UPDATE places SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;