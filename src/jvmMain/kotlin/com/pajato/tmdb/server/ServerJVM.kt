package com.pajato.tmdb.server

import com.pajato.tmdb.core.TmdbData
import com.pajato.tmdb.core.getListName
import io.javalin.Context
import io.javalin.Javalin
import io.javalin.JavalinEvent
import io.javalin.JavalinEvent.*
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass

internal const val SERVER_RUNNING_MESSAGE = "Server is running."
internal const val SERVER_STARTING_MESSAGE = "Server is starting."
internal const val SERVER_STOPPED_MESSAGE = "Server is stopped."
internal const val SERVER_STOPPING_MESSAGE = "Server is stopping."
internal const val SERVER_FAILED_MESSAGE = "Server start failed."

private const val ARG_LIST_NAME = "list"
private const val ARG_START = "start"
private const val ARG_SIZE = "size"
internal const val PORT = 7234

internal actual class Server(port: Int = PORT) {
    private val resourceDir by lazy { "${File(this::class.java.classLoader.getResource("").path).parent}/main" }
    private val cache: Map<String, List<String>> by lazy {
        fun getPair(kclass: KClass<out TmdbData>): Pair<String, List<String>> {
            val listName = kclass.getListName()
            val filteredFiles = File(resourceDir).listFiles { _, name -> name.accept(listName) }
            val file = if (filteredFiles.size == 1) filteredFiles[0] else File("no such file")

            return if (!file.exists() || !file.isFile) listName to listOf() else listName to file.readLines()
        }

        TmdbData::class.sealedSubclasses
            .filter { kClass ->  kClass.getListName() != "" }
            .map { kClass -> getPair(kClass) }.toMap()
    }

    init {
        // Ensure that the resource files are uncompressed. For all files ending in ".gz" uncompress then.
        File(resourceDir).listFiles().forEach {
            if (it.isFile && it.name.endsWith(".gz")) it.uncompress()
        }
    }

    internal var state: JavalinEvent = SERVER_START_FAILED
    private val app: Javalin = Javalin.create().apply {
        fun getPageFromContext(ctx: Context): String {
            fun getPageFromParams(listName: String, start: Int, pageSize: Int): String {
                val startIndex = if (start < 0) 0 else start
                fun getPageRecords(): String {
                    val list: List<String> = cache[listName] ?: return ""
                    val result = StringBuilder()
                    val endIndex = Math.min(list.size, startIndex + pageSize - 1)

                    for (index in startIndex .. endIndex) {
                        result.append("${list[index]}\n")
                    }
                    return result.toString()
                }

                return getPageRecords()
            }
            val listName = ctx.pathParam(ARG_LIST_NAME)
            val start = ctx.pathParam(ARG_START).toInt()
            val pageSize = ctx.pathParam(ARG_SIZE).toInt()

            return getPageFromParams(listName, start, pageSize)
        }

        event(SERVER_STARTING) { state = SERVER_STARTING }
        event(SERVER_STARTED) { state = SERVER_STARTED }
        event(SERVER_STOPPING) { state = SERVER_STOPPING }
        event(SERVER_STOPPED) { state = SERVER_STOPPED }
        start(port).apply {
            get("/") { ctx ->
                ctx.result("Hello World")
            }
            get("/page/:$ARG_LIST_NAME/:$ARG_START/:$ARG_SIZE") { ctx ->
                ctx.result(getPageFromContext(ctx))
            }
        }
    }

    internal fun status(): String = when (state) {
        SERVER_START_FAILED -> SERVER_FAILED_MESSAGE
        SERVER_STARTED -> SERVER_RUNNING_MESSAGE
        SERVER_STARTING -> SERVER_STARTING_MESSAGE
        SERVER_STOPPED -> SERVER_STOPPED_MESSAGE
        SERVER_STOPPING -> SERVER_STOPPING_MESSAGE
    }

    internal fun stopServer(): String {
        if (state != SERVER_STARTED) return status()
        app.stop()
        return status()
    }
}

// Extension functions

/** Uncompress and save a gzip compressed file ending in .gz using the base name and removing the .gz file. */
internal fun File.uncompress() {
    val outputStream = File(this.parentFile, this.nameWithoutExtension).outputStream()
    GZIPInputStream(this.inputStream()).apply { copyTo(outputStream) }
    this.inputStream().close()
    outputStream.close()
    this.delete()
}

/** Filename filter for valid resource files. */
internal fun String.accept(listName: String): Boolean = this.startsWith(listName) && this.endsWith(".json")