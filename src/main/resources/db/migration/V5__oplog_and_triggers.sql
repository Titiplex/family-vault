PRAGMA foreign_keys= ON;

-- Journal de modifications (INSERT/UPDATE/SOFT-DELETE)
CREATE TABLE IF NOT EXISTS oplog
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    table_name TEXT NOT NULL,
    uuid       TEXT,
    at         TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))
);

-- Helper pour créer triggers d'INSERT/UPDATE
-- users
CREATE TRIGGER IF NOT EXISTS op_users_ai
    AFTER INSERT
    ON users
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('users', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_users_au
    AFTER UPDATE
    ON users
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('users', NEW.uuid);
END;

-- lists
CREATE TRIGGER IF NOT EXISTS op_lists_ai
    AFTER INSERT
    ON lists
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('lists', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_lists_au
    AFTER UPDATE
    ON lists
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('lists', NEW.uuid);
END;

-- events
CREATE TRIGGER IF NOT EXISTS op_events_ai
    AFTER INSERT
    ON events
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('events', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_events_au
    AFTER UPDATE
    ON events
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('events', NEW.uuid);
END;

-- recipes
CREATE TRIGGER IF NOT EXISTS op_recipes_ai
    AFTER INSERT
    ON recipes
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('recipes', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_recipes_au
    AFTER UPDATE
    ON recipes
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('recipes', NEW.uuid);
END;

-- messages
CREATE TRIGGER IF NOT EXISTS op_messages_ai
    AFTER INSERT
    ON messages
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('messages', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_messages_au
    AFTER UPDATE
    ON messages
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('messages', NEW.uuid);
END;

-- photos
CREATE TRIGGER IF NOT EXISTS op_photos_ai
    AFTER INSERT
    ON photos
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('photos', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_photos_au
    AFTER UPDATE
    ON photos
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('photos', NEW.uuid);
END;

-- contacts
CREATE TRIGGER IF NOT EXISTS op_contacts_ai
    AFTER INSERT
    ON contacts
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('contacts', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_contacts_au
    AFTER UPDATE
    ON contacts
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('contacts', NEW.uuid);
END;

-- activities
CREATE TRIGGER IF NOT EXISTS op_activities_ai
    AFTER INSERT
    ON activities
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('activities', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_activities_au
    AFTER UPDATE
    ON activities
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('activities', NEW.uuid);
END;

-- documents
CREATE TRIGGER IF NOT EXISTS op_documents_ai
    AFTER INSERT
    ON documents
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('documents', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_documents_au
    AFTER UPDATE
    ON documents
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('documents', NEW.uuid);
END;

-- budget
CREATE TRIGGER IF NOT EXISTS op_budget_ai
    AFTER INSERT
    ON budget
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_budget_au
    AFTER UPDATE
    ON budget
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget', NEW.uuid);
END;

-- meals
CREATE TRIGGER IF NOT EXISTS op_meals_ai
    AFTER INSERT
    ON meals
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('meals', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_meals_au
    AFTER UPDATE
    ON meals
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('meals', NEW.uuid);
END;

-- timetable
CREATE TRIGGER IF NOT EXISTS op_timetable_ai
    AFTER INSERT
    ON timetable
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('timetable', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_timetable_au
    AFTER UPDATE
    ON timetable
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('timetable', NEW.uuid);
END;

-- places
CREATE TRIGGER IF NOT EXISTS op_places_ai
    AFTER INSERT
    ON places
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('places', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_places_au
    AFTER UPDATE
    ON places
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('places', NEW.uuid);
END;

CREATE INDEX IF NOT EXISTS idx_oplog_id ON oplog (id);
