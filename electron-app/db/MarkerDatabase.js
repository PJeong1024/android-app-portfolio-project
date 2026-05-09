'use strict'
const Database = require('better-sqlite3')
const path = require('path')

/**
 * SQLite-backed marker store.
 *
 * Schema mirrors the Android UserImg fields that Electron receives:
 *   image_id (PK), display_name, lat, long, thumbnail_data (base64), received_at
 *
 * All methods are synchronous (better-sqlite3 is sync-only).
 */
class MarkerDatabase {
  constructor(dataDir) {
    this._db = new Database(path.join(dataDir, 'markers.db'))
    this._db.pragma('journal_mode = WAL')
    this._init()
    this._prepareStatements()
  }

  _init() {
    this._db.exec(`
      CREATE TABLE IF NOT EXISTS markers (
        image_id          INTEGER PRIMARY KEY,
        display_name      TEXT    NOT NULL DEFAULT '',
        lat               REAL,
        long              REAL,
        thumbnail_data    TEXT,
        received_at       INTEGER NOT NULL DEFAULT 0
      )
    `)
  }

  _prepareStatements() {
    this._stmtInsert = this._db.prepare(`
      INSERT OR IGNORE INTO markers
        (image_id, display_name, lat, long, received_at)
      VALUES
        (@imageID, @displayName, @lat, @long, @receivedAt)
    `)

    this._stmtHasRow = this._db.prepare(
      'SELECT 1 FROM markers WHERE image_id = ?'
    )

    this._stmtHasThumbnail = this._db.prepare(
      'SELECT thumbnail_data FROM markers WHERE image_id = ?'
    )

    this._stmtUpdateThumbnail = this._db.prepare(
      'UPDATE markers SET thumbnail_data = ? WHERE image_id = ? AND thumbnail_data IS NULL'
    )

    this._stmtGetAll = this._db.prepare(
      'SELECT image_id, display_name, lat, long, thumbnail_data FROM markers ORDER BY received_at'
    )
  }

  /**
   * Inserts item only if imageID is not already in the DB.
   * Returns true if a new row was inserted.
   */
  insertIfAbsent(item) {
    const result = this._stmtInsert.run({
      imageID: item.imageID,
      displayName: item.imageDisplayName ?? '',
      lat: item.imageLat ?? null,
      long: item.imageLong ?? null,
      receivedAt: Date.now()
    })
    return result.changes > 0
  }

  /**
   * Saves thumbnail only if the row exists and has no thumbnail yet.
   * Returns true if updated.
   */
  saveThumbnailIfAbsent(imageID, thumbnailData) {
    const result = this._stmtUpdateThumbnail.run(thumbnailData, imageID)
    return result.changes > 0
  }

  /** Returns all markers as plain objects. */
  getAll() {
    return this._stmtGetAll.all().map((row) => ({
      imageID: row.image_id,
      imageDisplayName: row.display_name,
      imageLat: row.lat,
      imageLong: row.long,
      thumbnailData: row.thumbnail_data ?? null
    }))
  }

  close() {
    this._db.close()
  }
}

module.exports = { MarkerDatabase }
