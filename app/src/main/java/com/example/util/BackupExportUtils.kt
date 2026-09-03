package com.example.util

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.data.db.AppDatabase
import com.example.data.model.AppSetting
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupExportUtils {

    const val DOWNLOADS_SUBDIR = "digital-bahi-khata"

    fun getAppDownloadsDir(context: Context): File {
        val publicDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DOWNLOADS_SUBDIR)
        if (publicDownloads.exists() || publicDownloads.mkdirs()) {
            return publicDownloads
        }
        val fallback = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOADS_SUBDIR)
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }

    private fun scanMediaFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        } catch (_: Exception) {
        }
    }

    /**
     * Copies the active Room SQLite database file into /Downloads/digital-bahi-khata/ folder.
     */
    suspend fun createDatabaseBackup(context: Context): File? = withContext(Dispatchers.IO) {
        try {
            // Flush WAL journal into the main SQLite database file
            val db = AppDatabase.getInstance(context)
            val dbHelper = db.openHelper.writableDatabase
            val cursor = dbHelper.query("PRAGMA wal_checkpoint(TRUNCATE)")
            if (cursor != null) {
                cursor.moveToFirst()
                cursor.close()
            }

            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
            if (!dbFile.exists()) return@withContext null

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetDir = getAppDownloadsDir(context)

            val backupFile = File(targetDir, "BahiKhata_Backup_$timeStamp.db")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            scanMediaFile(context, backupFile)
            backupFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Verifies if URI is a .db file.
     */
    fun isDbFile(context: Context, uri: Uri): Boolean {
        try {
            var fileName: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            if (fileName != null) {
                return fileName!!.endsWith(".db", ignoreCase = true) || fileName!!.endsWith(".sqlite", ignoreCase = true)
            }
            val path = uri.path ?: ""
            return path.endsWith(".db", ignoreCase = true) || path.endsWith(".sqlite", ignoreCase = true)
        } catch (_: Exception) {
            return true
        }
    }

    /**
     * Validates an imported SQLite database without touching the current database.
     * The file is copied to cache first and checked independently with SQLite.
     */
    suspend fun validateDatabaseFile(context: Context, backupUri: Uri): String? = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("db_validation_", ".db", context.cacheDir)

        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return@withContext "चयनित डेटाबेस फ़ाइल खोली नहीं जा सकी।"

            // Read the identity hash from the CURRENT Room database before any replacement.
            val currentDb = AppDatabase.getInstance(context).openHelper.writableDatabase
            val expectedIdentityHash = readRoomIdentityHashFromSupportDb(currentDb)
                ?: return@withContext "वर्तमान Android database का Room identity hash नहीं मिला।"

            android.database.sqlite.SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).use { database ->

                database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (!cursor.moveToFirst() || cursor.getString(0) != "ok") {
                        return@withContext "SQLite integrity check विफल हुआ।"
                    }
                }

                val importedIdentityHash = readRoomIdentityHash(database)
                    ?: return@withContext "यह database Room database नहीं है: room_master_table नहीं मिली।"

                if (importedIdentityHash != expectedIdentityHash) {
                    return@withContext "Database schema इस Android app के Room schema से मेल नहीं खाता।"
                }

                val requiredTables = listOf("customers", "transactions", "app_settings")
                for (table in requiredTables) {
                    database.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
                        arrayOf(table)
                    ).use { cursor ->
                        if (!cursor.moveToFirst()) {
                            return@withContext "Required table missing: $table"
                        }
                    }
                }

                val requiredColumns = mapOf(
                    "customers" to listOf(
                        "id", "customer_code", "name", "mobile_number", "address", "created_at", "updated_at"
                    ),
                    "transactions" to listOf(
                        "id", "customer_id", "transaction_type", "amount_paise", "description",
                        "entry_date", "entry_time", "created_at", "updated_at"
                    ),
                    "app_settings" to listOf("key", "value")
                )

                for ((table, columns) in requiredColumns) {
                    database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        val actualColumns = mutableSetOf<String>()
                        while (cursor.moveToNext()) {
                            actualColumns.add(cursor.getString(nameIndex))
                        }
                        for (column in columns) {
                            if (column !in actualColumns) {
                                return@withContext "Column '$column' missing from table '$table'."
                            }
                        }
                    }
                }

                database.rawQuery(
                    """
                    SELECT COUNT(*)
                    FROM transactions
                    WHERE customer_id NOT IN (SELECT id FROM customers)
                    """.trimIndent(),
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                        return@withContext "कुछ transactions ऐसे customers को reference करते हैं जो मौजूद नहीं हैं।"
                    }
                }

                val customerCount = getTableCount(database, "customers")
                val transactionCount = getTableCount(database, "transactions")
                val settingCount = getTableCount(database, "app_settings")

                return@withContext """
                    Database is valid.
                    Customers: $customerCount
                    Transactions: $transactionCount
                    Settings: $settingCount
                """.trimIndent()
            }
        } catch (e: Exception) {
            "Database validation failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        } finally {
            tempFile.delete()
        }
    }

    private fun readRoomIdentityHashFromSupportDb(database: androidx.sqlite.db.SupportSQLiteDatabase): String? {
        return try {
            database.query(
                "SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1"
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readRoomIdentityHash(database: android.database.sqlite.SQLiteDatabase): String? {
        return try {
            database.rawQuery(
                "SELECT identity_hash FROM room_master_table WHERE id = 42 LIMIT 1",
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getTableCount(
        database: android.database.sqlite.SQLiteDatabase,
        tableName: String
    ): Int {
        database.rawQuery("SELECT COUNT(*) FROM $tableName", null).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    /**
     * Replaces the current Room database only after validation succeeds.
     * If Room cannot open the replacement, the previous database is restored automatically.
     */
    suspend fun restoreDatabaseBackup(context: Context, backupUri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
        val parentDir = dbFile.parentFile
        if (parentDir != null && !parentDir.exists()) parentDir.mkdirs()

        // IMPORTANT: validation happens before closing or deleting the current database.
        val validationResult = validateDatabaseFile(context, backupUri)
        if (validationResult == null || !validationResult.startsWith("Database is valid.")) {
            return@withContext false to (validationResult ?: "Database validation failed.")
        }

        val incomingFile = File.createTempFile("db_restore_", ".db", parentDir ?: context.cacheDir)
        val safetyFile = File.createTempFile("db_safety_", ".db", parentDir ?: context.cacheDir)

        try {
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(incomingFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return@withContext false to "चयनित database फ़ाइल खोली नहीं जा सकी।"

            // Make a rollback copy of the current database while Room is still open.
            if (dbFile.exists()) {
                AppDatabase.getInstance(context).openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(safetyFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
            }

            AppDatabase.resetDatabaseInstance()

            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            walFile.delete()
            shmFile.delete()

            incomingFile.copyTo(dbFile, overwrite = true)

            try {
                AppDatabase.getInstance(context).openHelper.writableDatabase
            } catch (_: Exception) {
                AppDatabase.resetDatabaseInstance()
                dbFile.delete()
                walFile.delete()
                shmFile.delete()

                if (safetyFile.exists()) {
                    safetyFile.copyTo(dbFile, overwrite = true)
                }

                AppDatabase.getInstance(context).openHelper.writableDatabase
                return@withContext false to "नया database Room में open नहीं हो सका। पुराना database सुरक्षित रूप से वापस restore कर दिया गया।"
            }

            true to validationResult
        } catch (e: Exception) {
            try {
                AppDatabase.resetDatabaseInstance()
                dbFile.delete()
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                if (safetyFile.exists()) safetyFile.copyTo(dbFile, overwrite = true)
                AppDatabase.getInstance(context).openHelper.writableDatabase
            } catch (_: Exception) {
            }

            false to "Restore विफल: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
        } finally {
            incomingFile.delete()
            safetyFile.delete()
        }
    }

    /**
     * Exports all database tables (Shop Details, Customers, Transactions) into a 100% compliant Microsoft Excel (.xlsx) file
     * with 3 separate worksheets:
     * Sheet 1: 1. दुकान विवरण
     * Sheet 2: 2. ग्राहक सूची
     * Sheet 3: 3. लेन-देन विवरण
     */
    suspend fun exportAllTablesToExcelCsv(
        context: Context,
        customers: List<Customer>,
        transactions: List<Transaction>,
        settings: List<AppSetting>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetDir = getAppDownloadsDir(context)
            val customerMap = customers.associateBy { it.id }

            val xlsxFile = File(targetDir, "BahiKhata_3Sheets_$timeStamp.xlsx")
            val zos = ZipOutputStream(FileOutputStream(xlsxFile))

            // 1. [Content_Types].xml
            val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>""".trimIndent()
            writeZipEntry(zos, "[Content_Types].xml", contentTypesXml)

            // 2. _rels/.rels
            val rootRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>""".trimIndent()
            writeZipEntry(zos, "_rels/.rels", rootRelsXml)

            // 3. docProps/core.xml
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val corePropsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <dc:title>Digital Bahi Khata Export</dc:title>
  <dc:creator>Digital Bahi Khata</dc:creator>
  <cp:lastModifiedBy>Digital Bahi Khata</cp:lastModifiedBy>
  <dcterms:created xsi:type="dcterms:W3CDTF">$nowIso</dcterms:created>
  <dcterms:modified xsi:type="dcterms:W3CDTF">$nowIso</dcterms:modified>
</cp:coreProperties>""".trimIndent()
            writeZipEntry(zos, "docProps/core.xml", corePropsXml)

            // 4. docProps/app.xml
            val appPropsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
  <Application>Microsoft Excel</Application>
  <DocSecurity>0</DocSecurity>
  <ScaleCrop>false</ScaleCrop>
  <HeadingPairs>
    <vt:vector size="2" baseType="variant">
      <vt:variant><vt:lpstr>Worksheets</vt:lpstr></vt:variant>
      <vt:variant><vt:i4>3</vt:i4></vt:variant>
    </vt:vector>
  </HeadingPairs>
  <TitlesOfParts>
    <vt:vector size="3" baseType="lpstr">
      <vt:lpstr>1. दुकान विवरण</vt:lpstr>
      <vt:lpstr>2. ग्राहक सूची</vt:lpstr>
      <vt:lpstr>3. लेन-देन विवरण</vt:lpstr>
    </vt:vector>
  </TitlesOfParts>
  <Company>Digital Bahi Khata</Company>
  <LinksUpToDate>false</LinksUpToDate>
  <SharedDoc>false</SharedDoc>
  <HyperlinksChanged>false</HyperlinksChanged>
  <AppVersion>16.0300</AppVersion>
</Properties>""".trimIndent()
            writeZipEntry(zos, "docProps/app.xml", appPropsXml)

            // 5. xl/_rels/workbook.xml.rels
            val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml)

            // 6. xl/workbook.xml (Declares 3 distinct sheets)
            val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <bookViews>
    <workbookView xWindow="0" yWindow="0" windowWidth="20480" windowHeight="10240"/>
  </bookViews>
  <sheets>
    <sheet name="1. दुकान विवरण" sheetId="1" r:id="rId1"/>
    <sheet name="2. ग्राहक सूची" sheetId="2" r:id="rId2"/>
    <sheet name="3. लेन-देन विवरण" sheetId="3" r:id="rId3"/>
  </sheets>
</workbook>""".trimIndent()
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)

            // 7. xl/styles.xml (Professional typography, header red fill, Udhar red, Jama green)
            val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="4">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFDC2626"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FF16A34A"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFB91C1C"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFCBD5E1"/></left>
      <right style="thin"><color rgb="FFCBD5E1"/></right>
      <top style="thin"><color rgb="FFCBD5E1"/></top>
      <bottom style="thin"><color rgb="FFCBD5E1"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="6">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <xf numFmtId="2" fontId="2" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyNumberFormat="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <xf numFmtId="2" fontId="3" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyNumberFormat="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <xf numFmtId="2" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyNumberFormat="1" applyAlignment="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
  </cellXfs>
  <cellStyles count="1">
    <cellStyle name="Normal" xfId="0" builtinId="0"/>
  </cellStyles>
</styleSheet>""".trimIndent()
            writeZipEntry(zos, "xl/styles.xml", stylesXml)

            // 8. xl/worksheets/sheet1.xml (SHEET 1: दुकान विवरण)
            val s1 = StringBuilder()
            s1.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" workbookViewId="0"/>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="18"/>
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/>
    <col min="2" max="2" width="22" customWidth="1"/>
    <col min="3" max="3" width="45" customWidth="1"/>
  </cols>
  <sheetData>
    <row r="1" ht="26" customHeight="1">
      <c r="A1" s="1" t="inlineStr"><is><t>विवरण (Setting Name)</t></is></c>
      <c r="B1" s="1" t="inlineStr"><is><t>सेटिंग कोड (Key)</t></is></c>
      <c r="C1" s="1" t="inlineStr"><is><t>मान (Value)</t></is></c>
    </row>
""")
            var r1 = 2
            for (st in settings) {
                val label = when (st.key) {
                    AppSetting.KEY_SHOP_NAME -> "दुकान / व्यापार का नाम"
                    AppSetting.KEY_OWNER_NAME -> "दुकानदार का नाम"
                    AppSetting.KEY_ADDRESS -> "दुकान का पता"
                    AppSetting.KEY_MOBILE -> "संपर्क मोबाइल नंबर"
                    else -> st.key
                }
                s1.append("""    <row r="$r1" ht="20" customHeight="1">
      <c r="A$r1" s="0" t="inlineStr"><is><t>${escapeXml(label)}</t></is></c>
      <c r="B$r1" s="4" t="inlineStr"><is><t>${escapeXml(st.key)}</t></is></c>
      <c r="C$r1" s="0" t="inlineStr"><is><t>${escapeXml(st.value)}</t></is></c>
    </row>
""")
                r1++
            }
            s1.append("""  </sheetData>
</worksheet>""")
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", s1.toString())

            // 9. xl/worksheets/sheet2.xml (SHEET 2: ग्राहक सूची)
            val s2 = StringBuilder()
            s2.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0"/>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="18"/>
  <cols>
    <col min="1" max="1" width="10" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="35" customWidth="1"/>
    <col min="6" max="6" width="22" customWidth="1"/>
  </cols>
  <sheetData>
    <row r="1" ht="26" customHeight="1">
      <c r="A1" s="1" t="inlineStr"><is><t>ID</t></is></c>
      <c r="B1" s="1" t="inlineStr"><is><t>ग्राहक कोड</t></is></c>
      <c r="C1" s="1" t="inlineStr"><is><t>ग्राहक का नाम</t></is></c>
      <c r="D1" s="1" t="inlineStr"><is><t>मोबाइल नंबर</t></is></c>
      <c r="E1" s="1" t="inlineStr"><is><t>पता / विवरण</t></is></c>
      <c r="F1" s="1" t="inlineStr"><is><t>खाता खोलने की तिथि</t></is></c>
    </row>
""")
            var r2 = 2
            for (c in customers) {
                s2.append("""    <row r="$r2" ht="20" customHeight="1">
      <c r="A$r2" s="4"><v>${c.id}</v></c>
      <c r="B$r2" s="4" t="inlineStr"><is><t>${escapeXml(c.customerCode)}</t></is></c>
      <c r="C$r2" s="0" t="inlineStr"><is><t>${escapeXml(c.name)}</t></is></c>
      <c r="D$r2" s="4" t="inlineStr"><is><t>${escapeXml(c.mobileNumber ?: "")}</t></is></c>
      <c r="E$r2" s="0" t="inlineStr"><is><t>${escapeXml(c.address ?: "")}</t></is></c>
      <c r="F$r2" s="4" t="inlineStr"><is><t>${escapeXml(c.createdAt)}</t></is></c>
    </row>
""")
                r2++
            }
            s2.append("""  </sheetData>
</worksheet>""")
            writeZipEntry(zos, "xl/worksheets/sheet2.xml", s2.toString())

            // 10. xl/worksheets/sheet3.xml (SHEET 3: लेन-देन विवरण)
            val s3 = StringBuilder()
            s3.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0"/>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="18"/>
  <cols>
    <col min="1" max="1" width="10" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="28" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
    <col min="5" max="5" width="18" customWidth="1"/>
    <col min="6" max="6" width="35" customWidth="1"/>
    <col min="7" max="7" width="16" customWidth="1"/>
    <col min="8" max="8" width="14" customWidth="1"/>
    <col min="9" max="9" width="22" customWidth="1"/>
  </cols>
  <sheetData>
    <row r="1" ht="26" customHeight="1">
      <c r="A1" s="1" t="inlineStr"><is><t>ID</t></is></c>
      <c r="B1" s="1" t="inlineStr"><is><t>ग्राहक कोड</t></is></c>
      <c r="C1" s="1" t="inlineStr"><is><t>ग्राहक का नाम</t></is></c>
      <c r="D1" s="1" t="inlineStr"><is><t>प्रकार (Type)</t></is></c>
      <c r="E1" s="1" t="inlineStr"><is><t>राशि रुपये (₹)</t></is></c>
      <c r="F1" s="1" t="inlineStr"><is><t>विवरण / सामान</t></is></c>
      <c r="G1" s="1" t="inlineStr"><is><t>दिनांक</t></is></c>
      <c r="H1" s="1" t="inlineStr"><is><t>समय</t></is></c>
      <c r="I1" s="1" t="inlineStr"><is><t>प्रविष्टि समय</t></is></c>
    </row>
""")
            var r3 = 2
            for (t in transactions) {
                val c = customerMap[t.customerId]
                val typeHindi = if (t.isCredit) "उधार" else "जमा"
                val amountStyle = if (t.isCredit) "2" else "3"
                val rawRupeesValue = String.format(Locale.US, "%.2f", t.amountPaise / 100.0)

                s3.append("""    <row r="$r3" ht="20" customHeight="1">
      <c r="A$r3" s="4"><v>${t.id}</v></c>
      <c r="B$r3" s="4" t="inlineStr"><is><t>${escapeXml(c?.customerCode ?: "")}</t></is></c>
      <c r="C$r3" s="0" t="inlineStr"><is><t>${escapeXml(c?.name ?: "")}</t></is></c>
      <c r="D$r3" s="4" t="inlineStr"><is><t>$typeHindi</t></is></c>
      <c r="E$r3" s="$amountStyle"><v>$rawRupeesValue</v></c>
      <c r="F$r3" s="0" t="inlineStr"><is><t>${escapeXml(t.description ?: "")}</t></is></c>
      <c r="G$r3" s="4" t="inlineStr"><is><t>${escapeXml(t.entryDate)}</t></is></c>
      <c r="H$r3" s="4" t="inlineStr"><is><t>${escapeXml(t.entryTime)}</t></is></c>
      <c r="I$r3" s="4" t="inlineStr"><is><t>${escapeXml(t.createdAt)}</t></is></c>
    </row>
""")
                r3++
            }
            s3.append("""  </sheetData>
</worksheet>""")
            writeZipEntry(zos, "xl/worksheets/sheet3.xml", s3.toString())

            zos.flush()
            zos.close()

            scanMediaFile(context, xlsxFile)
            xlsxFile
        } catch (_: Exception) {
            null
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, path: String, content: String) {
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun escapeXml(value: String): String {
        val clean = StringBuilder()
        for (ch in value) {
            when (ch) {
                '&' -> clean.append("&amp;")
                '<' -> clean.append("&lt;")
                '>' -> clean.append("&gt;")
                '"' -> clean.append("&quot;")
                '\'' -> clean.append("&apos;")
                '\t', '\n', '\r' -> clean.append(ch)
                else -> {
                    if (ch.code >= 32) {
                        clean.append(ch)
                    }
                }
            }
        }
        return clean.toString()
    }

    fun shareFile(context: Context, file: File, mimeType: String, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, subject))
        } catch (_: Exception) {
        }
    }
}