PRAGMA foreign_keys = ON;

/* --- Catégories --- */
CREATE TABLE IF NOT EXISTS budget_categories
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    name       TEXT    NOT NULL,
    color      TEXT,
    uuid       TEXT,
    updated_at TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted    INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_categories_u ON budget_categories (user_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_categories_uuid ON budget_categories (uuid);
CREATE INDEX IF NOT EXISTS idx_budget_categories_updated ON budget_categories (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_budget_categories_ai
    AFTER INSERT
    ON budget_categories
BEGIN
    UPDATE budget_categories SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_budget_categories_au
    AFTER UPDATE
    ON budget_categories
BEGIN
    UPDATE budget_categories SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* --- Taux FX historiques --- */
CREATE TABLE IF NOT EXISTS fx_rates
(
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    base       TEXT    NOT NULL, -- ex: EUR
    quote      TEXT    NOT NULL, -- ex: USD
    rate       REAL    NOT NULL, -- 1 base = rate quote
    at         TEXT    NOT NULL, -- date du taux (UTC ISO)
    uuid       TEXT,
    updated_at TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fx_rates_pair_date ON fx_rates (base, quote, at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_fx_rates_uuid ON fx_rates (uuid);
CREATE TRIGGER IF NOT EXISTS trg_fx_rates_ai
    AFTER INSERT
    ON fx_rates
BEGIN
    UPDATE fx_rates SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_fx_rates_au
    AFTER UPDATE
    ON fx_rates
BEGIN
    UPDATE fx_rates SET updated_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* --- Paramètres utilisateur (devise par défaut) --- */
CREATE TABLE IF NOT EXISTS user_settings
(
    user_id          INTEGER PRIMARY KEY,
    default_currency TEXT NOT NULL
);

/* --- Transactions budget --- */
CREATE TABLE IF NOT EXISTS budget_tx
(
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL,           -- auteur de la saisie (propriétaire)
    tx_date         TEXT    NOT NULL,           -- date de la dépense/recette
    category_id     INTEGER,                    -- FK catégories
    payer_user_id   INTEGER NOT NULL,           -- qui a payé
    currency        TEXT    NOT NULL,           -- devise de saisie (ex: USD)
    amount          REAL    NOT NULL,           -- montant dans la devise de saisie
    rate_to_default REAL    NOT NULL,           -- taux vers devise par défaut au moment T
    amount_default  REAL    NOT NULL,           -- montant converti (pour comparaisons/limites)
    note            TEXT,
    is_income       INTEGER NOT NULL DEFAULT 0, -- 0=dépense, 1=recette
    uuid            TEXT,
    updated_at      TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted         INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES budget_categories (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_budget_tx_user_date ON budget_tx (user_id, tx_date);
CREATE INDEX IF NOT EXISTS idx_budget_tx_cat ON budget_tx (category_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_tx_uuid ON budget_tx (uuid);
CREATE INDEX IF NOT EXISTS idx_budget_tx_updated ON budget_tx (updated_at);
CREATE TRIGGER IF NOT EXISTS trg_budget_tx_ai
    AFTER INSERT
    ON budget_tx
BEGIN
    UPDATE budget_tx SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_budget_tx_au
    AFTER UPDATE
    ON budget_tx
BEGIN
    UPDATE budget_tx SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* Journal des parts par participant (pour répartition) */
CREATE TABLE IF NOT EXISTS budget_tx_participants
(
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    tx_id               INTEGER NOT NULL,
    participant_user_id INTEGER NOT NULL,
    share_percent       REAL    NOT NULL, -- somme 100.0
    uuid                TEXT,
    updated_at          TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted             INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (tx_id) REFERENCES budget_tx (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_budget_tx_part_tx ON budget_tx_participants (tx_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_tx_part_uuid ON budget_tx_participants (uuid);
CREATE TRIGGER IF NOT EXISTS trg_budget_tx_part_ai
    AFTER INSERT
    ON budget_tx_participants
BEGIN
    UPDATE budget_tx_participants SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_budget_tx_part_au
    AFTER UPDATE
    ON budget_tx_participants
BEGIN
    UPDATE budget_tx_participants SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* Limites & objectifs (par période) */
CREATE TABLE IF NOT EXISTS budget_limits
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    category_id  INTEGER,                          -- NULL = global
    period_start TEXT    NOT NULL,
    period_end   TEXT    NOT NULL,
    amount_limit REAL    NOT NULL,                 -- devise par défaut de l'user
    goal_type    TEXT             DEFAULT 'spend', -- 'spend' (plafond) ou 'save' (objectif d'épargne)
    uuid         TEXT,
    updated_at   TEXT             DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
    deleted      INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES budget_categories (id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_budget_limits_user_period ON budget_limits (user_id, period_start, period_end);
CREATE UNIQUE INDEX IF NOT EXISTS idx_budget_limits_uuid ON budget_limits (uuid);
CREATE TRIGGER IF NOT EXISTS trg_budget_limits_ai
    AFTER INSERT
    ON budget_limits
BEGIN
    UPDATE budget_limits SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;
CREATE TRIGGER IF NOT EXISTS trg_budget_limits_au
    AFTER UPDATE
    ON budget_limits
BEGIN
    UPDATE budget_limits SET updated_at=strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE rowid = NEW.rowid;
END;

/* --- Oplog (auto-sync) pour les nouvelles tables --- */
CREATE TRIGGER IF NOT EXISTS op_budget_tx_ai
    AFTER INSERT
    ON budget_tx
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_tx', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_budget_tx_au
    AFTER UPDATE
    ON budget_tx
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_tx', NEW.uuid);
END;

CREATE TRIGGER IF NOT EXISTS op_budget_tx_part_ai
    AFTER INSERT
    ON budget_tx_participants
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_tx_participants', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_budget_tx_part_au
    AFTER UPDATE
    ON budget_tx_participants
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_tx_participants', NEW.uuid);
END;

CREATE TRIGGER IF NOT EXISTS op_budget_categories_ai
    AFTER INSERT
    ON budget_categories
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_categories', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_budget_categories_au
    AFTER UPDATE
    ON budget_categories
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_categories', NEW.uuid);
END;

CREATE TRIGGER IF NOT EXISTS op_fx_rates_ai
    AFTER INSERT
    ON fx_rates
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('fx_rates', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_fx_rates_au
    AFTER UPDATE
    ON fx_rates
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('fx_rates', NEW.uuid);
END;

CREATE TRIGGER IF NOT EXISTS op_budget_limits_ai
    AFTER INSERT
    ON budget_limits
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_limits', NEW.uuid);
END;
CREATE TRIGGER IF NOT EXISTS op_budget_limits_au
    AFTER UPDATE
    ON budget_limits
BEGIN
    INSERT INTO oplog(table_name, uuid) VALUES ('budget_limits', NEW.uuid);
END;
