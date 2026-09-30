package com.unicornwhodev.visiondatasetstudio

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/** Opt-in read-only logical snapshot before/after an update. No user contents leave the device. */
class InstalledDatabaseDigestTest {
    private fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    @Test fun snapshotAllInstalledTablesWithoutChangingThem() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val path=instrumentation.targetContext.getDatabasePath("vision_dataset_studio.db")
        assertTrue("Installed database missing",path.isFile)
        val receipt=JSONObject()
        SQLiteDatabase.openDatabase(path.path,null,SQLiteDatabase.OPEN_READONLY).use { db ->
            receipt.put("schema_version",db.version)
            val tables=mutableListOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null).use { c ->
                while(c.moveToNext())tables.add(c.getString(0))
            }
            val rows=JSONObject()
            for(table in tables) {
                require(table.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")))
                val hashes=mutableListOf<String>();val columns=JSONArray()
                db.rawQuery("SELECT * FROM \"$table\"",null).use { c ->
                    c.columnNames.forEach { columns.put(it) }
                    while(c.moveToNext()) {
                        val row=JSONArray()
                        for(index in 0 until c.columnCount) {
                            val value=when(c.getType(index)) {
                                android.database.Cursor.FIELD_TYPE_NULL -> "null"
                                android.database.Cursor.FIELD_TYPE_BLOB -> "blob:"+sha(c.getBlob(index))
                                android.database.Cursor.FIELD_TYPE_INTEGER -> "int:"+c.getLong(index)
                                android.database.Cursor.FIELD_TYPE_FLOAT -> "float:"+c.getDouble(index)
                                else -> "text:"+c.getString(index)
                            }
                            row.put(value)
                        }
                        hashes.add(sha(row.toString().toByteArray(Charsets.UTF_8)))
                    }
                }
                rows.put(table,JSONObject().put("rows",hashes.size).put("columns",columns)
                    .put("rows_sha256",sha(hashes.sorted().joinToString("\n").toByteArray(Charsets.UTF_8))))
            }
            receipt.put("tables",rows)
        }
        requireNotNull(instrumentation.targetContext.contentResolver.openOutputStream(android.net.Uri.parse(
            "content://com.unicornwhodev.visiondatasetstudio.test.qa-evidence/installed-database-digest.json"),"w"))
            .use { it.write(receipt.toString(2).toByteArray(Charsets.UTF_8)) }
        instrumentation.sendStatus(0,Bundle().apply { putString("installed_database_digest",receipt.toString()) })
    }
}
