import com.pajato.tmdb.server.ContextImpl
import com.pajato.tmdb.server.getLastExportDate
import com.pajato.tmdb.server.getResourceFileForList
import com.pajato.tmdb.server.module
import com.soywiz.klock.DateTime
import io.ktor.application.install
import io.ktor.features.HttpsRedirect
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class UpdaterTest {
    private val dir = object {}.javaClass.classLoader.getResource(".") ?: URL("http://")
    private val updaterContext = ContextImpl(
        cycles = 10,
        baseUrl = dir.toString(),
        exportDate = "03_15_2019",
        readTimeoutMillis = 50,
        connectTimeoutMillis = 20,
        updateIntervalMillis = 100L
    )

    @Test fun `test that 10 cycles worth of updates works correctly`() {
        withTestApplication({ module(testing = true, context = updaterContext) }) {
            application.install(XForwardedHeaderSupport)
            application.install(HttpsRedirect)
            runBlocking {
                delay(15 * updaterContext.updateIntervalMillis)
                assertEquals(updaterContext.cycles, updaterContext.counter, "Wrong number of cycles executed!")
            }
        }
    }

    @Test fun `when the current time is midnight verify the last export date is correct`() {
        val midnight = 0L
        val noon = midnight + 12L * 60 * 60 * 1000
        assertEquals("12_31_1969", getLastExportDate(DateTime(midnight)), "Incorrect last export date!")
        assertEquals("01_01_1970", getLastExportDate(DateTime(noon)), "Incorrect last export date!")
    }

    @Test fun `when the updater cycle count is 1 verify the update action method code coverage is complete`() {
        val updaterContext = ContextImpl(
            cycles = -1,
            baseUrl = dir.toString(),
            exportDate = "03_15_2019",
            readTimeoutMillis = 50,
            connectTimeoutMillis = 20,
            updateIntervalMillis = 100L
        )
        assertTrue(updaterContext.updateAction(), "Updater action handler return value is wrong!")
    }

    @Test fun `when an exception occurs in the getResourceFileForList method verify the result file`() {
        val updaterContext = ContextImpl(
            cycles = -1,
            baseUrl = "/no such url",
            exportDate = "03_15_2019",
            readTimeoutMillis = 50,
            connectTimeoutMillis = 20,
            updateIntervalMillis = 100L
        )
        val file = getResourceFileForList("movie_ids", updaterContext, File("/tmp"))
        assertTrue(file.path.startsWith("No such file"), "Invalid base url did not generate an error!")
    }
}
