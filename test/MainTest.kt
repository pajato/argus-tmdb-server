import com.pajato.tmdb.server.main
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Test

class MainTest {
    @KtorExperimentalAPI
    @Test
    fun testThatTheServerTerminates() {
        val scope = CoroutineScope(context = Job() + Dispatchers.Default).apply {
            launch { launch(Dispatchers.Default) {
                println("Running main from coroutine.")
                main(arrayOf()) }
            }
        }
        scope.cancel()
    }
}