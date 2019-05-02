package com.pajato.tmdb.server

import io.javalin.JavalinEvent
import org.junit.Test
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

class LibraryTestJVM {

    private lateinit var server: Server

    @BeforeTest fun setUp() {
        server = Server()
        assertEquals(SERVER_RUNNING_MESSAGE, server.status(), "Wrong initial state!")
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

    @Test fun `when the server is queried for the first page verify that page size records is the response`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/movie_ids/0/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(pageSize, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `when the server is queried for a page with an invalid list name verify wrong page size response`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/no_such_ids/0/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(0, content.size, "Incorrect number of records in the response!")
    }

    @Test fun `when the server is queried with a negative start index verify the correct response`() {
        val pageSize = 25
        val url = "http://localhost:$PORT/page/movie_ids/-1/$pageSize"
        val content = URL(url).openConnection().getInputStream().reader().readLines()
        assertEquals(pageSize, content.size, "Incorrect number of records in the response!")
    }

    @Test
    fun `exercise the server status for coverage`() {
        server.stopServer()
        server.state = JavalinEvent.SERVER_START_FAILED
        assertEquals(SERVER_FAILED_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STARTING
        assertEquals(SERVER_STARTING_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STOPPING
        assertEquals(SERVER_STOPPING_MESSAGE, server.status(), "Incorrect status message!")
        server.state = JavalinEvent.SERVER_STOPPED
    }
}
