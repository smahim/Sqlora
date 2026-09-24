package com.example

import com.example.data.sqlite.MetadataExtractor
import com.example.data.sqlite.SqlErrorParser
import com.example.data.sqlite.SqliteConnection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SqliteEngineTest {

    private lateinit var tempFile: File
    private lateinit var connection: SqliteConnection

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_sqlite_", ".db")
        connection = SqliteConnection(tempFile)
    }

    @After
    fun tearDown() = runBlocking {
        connection.close()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun testDatabaseLifecycle() = runBlocking {
        assertTrue(connection.open())
        assertTrue(connection.isOpen)

        connection.close()
        assertFalse(connection.isOpen)

        assertTrue(connection.reopen())
        assertTrue(connection.isOpen)
    }

    @Test
    fun testCreateAndQueryTable() = runBlocking {
        connection.open()

        // 1. Create table DDL
        val createResult = connection.executeSql(
            """
            CREATE TABLE inventory (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                quantity INTEGER DEFAULT 0,
                price REAL
            );
            """.trimIndent()
        )
        assertTrue(createResult.isSuccess)
        assertFalse(createResult.isSelect)

        // 2. Insert records
        val insert1 = connection.executeSql("INSERT INTO inventory (title, quantity, price) VALUES ('Widget A', 42, 9.99);")
        assertTrue(insert1.isSuccess)

        val insert2 = connection.executeSql("INSERT INTO inventory (title, quantity, price) VALUES ('Gadget B', 15, 24.50);")
        assertTrue(insert2.isSuccess)

        // 3. Real SELECT query
        val selectResult = connection.executeSql("SELECT id, title, quantity, price FROM inventory ORDER BY id ASC;")
        assertTrue(selectResult.isSuccess)
        assertTrue(selectResult.isSelect)
        assertEquals(listOf("id", "title", "quantity", "price"), selectResult.columns)
        assertEquals(2, selectResult.rows.size)

        // Verify row values
        val firstRow = selectResult.rows[0]
        assertEquals("1", firstRow[0])
        assertEquals("Widget A", firstRow[1])
        assertEquals("42", firstRow[2])
        assertEquals("9.99", firstRow[3])
    }

    @Test
    fun testMetadataIntrospection() = runBlocking {
        connection.open()

        // Create parent and child table with FK and Index
        connection.executeSql(
            """
            CREATE TABLE departments (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL UNIQUE
            );
            CREATE TABLE employees (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                dept_id INTEGER,
                FOREIGN KEY (dept_id) REFERENCES departments(id) ON DELETE CASCADE
            );
            CREATE INDEX idx_emp_dept ON employees(dept_id);
            CREATE VIEW emp_view AS SELECT id, name FROM employees;
            """.trimIndent()
        )

        val metadata = connection.getMetadata()
        assertNotNull(metadata)
        assertEquals(2, metadata.tables.size)
        assertEquals(1, metadata.views.size)

        val empTable = metadata.tables.find { it.name == "employees" }
        assertNotNull(empTable)
        assertEquals(listOf("id"), empTable!!.primaryKeys)

        // Verify foreign keys
        assertEquals(1, empTable.foreignKeys.size)
        assertEquals("departments", empTable.foreignKeys[0].targetTable)
        assertEquals("dept_id", empTable.foreignKeys[0].fromColumn)
        assertEquals("id", empTable.foreignKeys[0].toColumn)

        // Verify indexes
        assertTrue(empTable.indexes.any { it.name == "idx_emp_dept" })

        // Verify view
        assertEquals("emp_view", metadata.views[0].name)
    }

    @Test
    fun testConstraintViolationErrorDiagnosis() = runBlocking {
        connection.open()

        connection.executeSql(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                username TEXT NOT NULL UNIQUE
            );
            """.trimIndent()
        )

        connection.executeSql("INSERT INTO users (username) VALUES ('alice');")

        // Duplicate insert should fail with UNIQUE constraint violation
        val failedResult = connection.executeSql("INSERT INTO users (username) VALUES ('alice');")
        assertFalse(failedResult.isSuccess)
        assertNotNull(failedResult.errorDetails)
        assertEquals("Unique Constraint Violation", failedResult.errorDetails?.title)
        assertTrue(failedResult.errorDetails?.userExplanation?.contains("UNIQUE") == true)
        assertTrue(failedResult.errorDetails?.technicalMessage?.isNotEmpty() == true)
    }

    @Test
    fun testSyntaxErrorDiagnosis() = runBlocking {
        connection.open()

        val invalidResult = connection.executeSql("SELEC * FORM invalid_syntax;")
        assertFalse(invalidResult.isSuccess)
        assertNotNull(invalidResult.errorDetails)
        assertEquals("SQL Syntax Error", invalidResult.errorDetails?.title)
    }
}
