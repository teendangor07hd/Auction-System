CREATE TABLE IF NOT EXISTS users (
                                     id           TEXT    PRIMARY KEY,
                                     username     TEXT    UNIQUE NOT NULL,
                                     password_hash TEXT   NOT NULL,
                                     email        TEXT    NOT NULL,
                                     role         TEXT    NOT NULL,
                                     extra_int    INTEGER NOT NULL DEFAULT 0,
                                     created_at   TEXT    NOT NULL,
                                     updated_at   TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS items (
                                     id            TEXT PRIMARY KEY,
                                     name          TEXT NOT NULL,
                                     description   TEXT NOT NULL DEFAULT '',
                                     starting_price REAL NOT NULL,
                                     item_type     TEXT NOT NULL,
                                     seller_id     TEXT NOT NULL,
                                     extra_data    TEXT NOT NULL DEFAULT '{}',
                                     created_at    TEXT NOT NULL,
                                     updated_at    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS auctions (
                                        id                  TEXT PRIMARY KEY,
                                        item_id             TEXT NOT NULL,
                                        start_time          TEXT NOT NULL,
                                        end_time            TEXT NOT NULL,
                                        starting_price      REAL NOT NULL,
                                        current_highest_bid REAL NOT NULL,
                                        highest_bidder_id   TEXT,
                                        status              TEXT NOT NULL,
                                        minimum_increment   REAL NOT NULL DEFAULT 1.0,
                                        created_at          TEXT NOT NULL,
                                        updated_at          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bid_transactions (
                                                id         TEXT PRIMARY KEY,
                                                auction_id TEXT NOT NULL,
                                                bidder_id  TEXT NOT NULL,
                                                bid_amount REAL NOT NULL,
                                                bid_time   TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
                                          id         TEXT PRIMARY KEY,
                                          user_id    TEXT,
                                          action     TEXT NOT NULL,
                                          details    TEXT NOT NULL DEFAULT '',
                                          created_at TEXT NOT NULL
);