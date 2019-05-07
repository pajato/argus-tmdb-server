package com.pajato.tmdb.server

import com.pajato.tmdb.core.Movie
import com.soywiz.klock.DateTime
import io.javalin.JavalinEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryTestJVM {

    private lateinit var server: Server
    private val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
    private val context = ContextImpl(
        cycles = 10,
        baseUrl = dir.toString(),
        exportDate = "03_15_2019",
        readTimeoutMillis = 50,
        connectTimeoutMillis = 20,
        updateIntervalMillis = 100L
    )

    @BeforeTest fun setUp() {
        server = Server(context = context)
        assertEquals(SERVER_RUNNING_MESSAGE, server.status(), "Wrong initial state!")
        File(server.resourceDir).listFiles().forEach { if (it.isFile) it.delete() }
    }

    @AfterTest fun tearDown() {
        val stopResult = server.stopServer()
        assertEquals(SERVER_STOPPED_MESSAGE, stopResult, "Unexpected response stopping server!")
        assertEquals(SERVER_STOPPED_MESSAGE, server.status(), "Invalid state!")
    }

    @Test fun `when the server is queried with the default path verify hello world is the response`() {
        val url = "http://localhost:$PORT/"
        val content = URL(url).openConnection().getInputStream().reader().readText()
        assertEquals("Hello World", content, "Incorrect response!")
    }

    @Test fun `when the server is queried for the first two pages verify correct responses`() {
        val pageSize = 25
        val url0 = "http://localhost:$PORT/page/tv_network_ids/0/$pageSize"
        val content0 = URL(url0).openConnection().getInputStream().reader().readLines()
        assertEquals(pageSize, content0.size, "Incorrect number of records in the response!")
        val url1 = "http://localhost:$PORT/page/tv_network_ids/25/$pageSize"
        val content1 = URL(url1).openConnection().getInputStream().reader().readLines()
        assertEquals(pageSize, content1.size, "Incorrect number of records in the response!")
    }

    @Test fun `when the server is queried for a page with an invalid list name verify wrong page size response`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/no_such_ids/0/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(1, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `when the server is queried with a negative start index verify the correct response`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/tv_network_ids/-1/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(25, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `exercise the server status for coverage`() {
        server.stopServer()
        server.state = JavalinEvent.SERVER_START_FAILED
        assertEquals(SERVER_FAILED_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STARTING
        assertEquals(SERVER_STARTING_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STOPPING
        assertEquals(SERVER_STOPPING_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STOPPED
    }

    @Test fun `validate the initial cached resource file state`() {
        assertEquals(SERVER_RUNNING_MESSAGE, server.status(), "Bad initial status!")
    }

    @Test fun `test that 10 cycles worth of updates works correctly`() {
        runBlocking {
            delay(15 * context.updateIntervalMillis)
            assertEquals(context.cycles, context.counter, "Wrong number of cycles executed!")
        }
    }

    @Test fun `when the tv network resource file is missing verify it is reloaded`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/tv_network_ids/0/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(pageSize, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `when the current time is midnight verify the last export date is correct`() {
        val midnight = 0L
        val noon = midnight + 12L * 60 * 60 * 1000
        assertEquals("12_31_1969", getLastExportDate(DateTime(midnight)), "Incorrect last export date!")
        assertEquals("01_01_1970", getLastExportDate(DateTime(noon)), "Incorrect last export date!")
    }

    @Test fun `force a validation exception for coverage`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/foo_ids/0/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(1, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `exercise the accept extension for coverage`() {
        val listName = Movie.listName
        // Case 1:4 Starts with is false and ends with is false.
        assertFalse("fred".accept(listName), "Accepted incorrectly!")
        // Case 2:4 Starts with is true and ends with is false.
        assertFalse("movie_ids_....".accept(listName), "Accepted incorrectly!")
        // Case 3:4 Starts with is false and ends with is true.
        assertFalse("fred...json".accept(listName), "Accepted incorrectly!")
        // Case 4:4 Starts with is true and ends with is true.
        assertTrue("movie_ids_...json".accept(listName), "Not accepted!")
    }

}

