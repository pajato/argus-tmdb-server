import com.pajato.tmdb.server.module
import io.ktor.application.install
import io.ktor.features.HttpsRedirect
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod.Companion.Get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ApplicationTest {
    private var engineIsRunning = false

    @KtorExperimentalAPI
    @BeforeTest
    fun setup() {
        println("Settng up for test...")
        if (engineIsRunning) return
        com.pajato.tmdb.server.generateCertificate()
        engineIsRunning = true
    }

    @Test
    fun testRoot() {
        withTestApplication( { module(testing = true) }) {
            application.install(XForwardedHeaderSupport)
            application.install(HttpsRedirect)
            handleRequest(Get, "/") { addHeader(HttpHeaders.XForwardedProto, "https") }.let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
                val message = call.response.content ?: fail("No welcome message detected!")
                assertTrue(message.startsWith("Welcome"), "Welcome message is wrong!")
            }
        }
    }

    @Test
    fun testPaging() {
        println("Running paging test...")
        withTestApplication( { module(testing = true) }) {
            application.install(XForwardedHeaderSupport)
            application.install(HttpsRedirect)
            val pagingUrl = "/page/movie_ids/0/25"
            handleRequest(Get, pagingUrl) {
                addHeader(HttpHeaders.XForwardedProto, "https") }.let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
                val data = call.response.content ?: fail("No page data returned!")
                assertTrue(data.isNotBlank() && data.startsWith("{"), "Movie list format error!")
            }
        }
    }
}
