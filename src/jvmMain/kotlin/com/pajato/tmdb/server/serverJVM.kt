package com.pajato.tmdb.server

import io.javalin.Context
import io.javalin.Javalin
import io.javalin.JavalinEvent
import io.javalin.JavalinEvent.SERVER_STARTED
import io.javalin.JavalinEvent.SERVER_STARTING
import io.javalin.JavalinEvent.SERVER_START_FAILED
import io.javalin.JavalinEvent.SERVER_STOPPED
import io.javalin.JavalinEvent.SERVER_STOPPING
import java.io.File

internal const val SERVER_RUNNING_MESSAGE = "Server is running."
internal const val SERVER_STARTING_MESSAGE = "Server is starting."
internal const val SERVER_STOPPED_MESSAGE = "Server is stopped."
internal const val SERVER_STOPPING_MESSAGE = "Server is stopping."
internal const val SERVER_FAILED_MESSAGE = "Server start failed."

private const val ARG_LIST_NAME = "list"
private const val ARG_START = "start"
private const val ARG_SIZE = "size"
internal const val PORT = 7234

fun main() { Server() }

internal actual class Server(private val port: Int = PORT, private val context: FetchContext = ContextImpl()) {
    internal var state: JavalinEvent = SERVER_START_FAILED
    internal val resourceDir by lazy { "${File(this::class.java.classLoader.getResource("").path).parent}/main" }
    private val cache: MutableMap<String, List<String>> = mutableMapOf()
    private val app: Javalin by lazy { startServer() }

    init {
        processCacheUpdate(context, cache, File(resourceDir))
        app.port() // to force load the app
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

    private fun startServer(): Javalin = Javalin.create().apply {
        fun getPageFromContext(ctx: Context): String {
            fun getPageFromParams(listName: String, start: Int, pageSize: Int): String {
                fun getPageRecords(): String {
                    fun updateCache(): List<String> {
                        val resourceFile = getResourceFileForList(listName, context, File(resourceDir))

                        if (resourceFile.isFile) cache[listName] = resourceFile.readLines()
                        return cache[listName] ?: listOf("Could not load $listName from resource file: {${resourceFile.path}}!")
                    }
                    val list: List<String> = cache[listName] ?: updateCache()
                    val result = StringBuilder()
                    val startIndex = if (start < 0) 0 else start
                    val endIndex = Math.min(list.size - 1, startIndex + pageSize - 1)

                    for (index in startIndex .. endIndex) { result.append("${list[index]}\n") }
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
}

// Extension functions

/** Filename filter for valid resource files. */
internal fun String.accept(listName: String): Boolean = this.startsWith(listName) && this.endsWith(".json")
