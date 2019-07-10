package com.pajato.tmdb.server

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.HttpsRedirect
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul
import java.nio.file.Paths

/** See Ktor documentation for Application ... */
fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false, context: FetchContext = ContextImpl()) {
    val resourceDir by lazy { Paths.get(this::class.java.classLoader.getResource("datasets").toURI()).toFile() }
    val cache: MutableMap<String, List<String>> = mutableMapOf()
    fun getHtml(html: HTML) {
        html.body {
            h1 { +"HTML" }
            ul { for (n in 1..10) { li { +"$n" } } }
        }
    }
    fun getIndexedPage(parameters: Parameters): String {
        fun getPageFromParams(listName: String, start: Int, pageSize: Int): String {
            fun getPageRecords(): String {
                fun updateCache(): List<String> {
                    val resourceFile = getResourceFileForList(listName, context, resourceDir)

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

        val listName = parameters["listName"] ?: return "Error: missing list name"
        val pageStart = parameters["pageStart"]?.toInt() ?: return "Error: invalid or missing missing page start value"
        val pageSize = parameters["pageSize"]?.toInt() ?: return "Error: invalid or missing page size value"
        return getPageFromParams(listName, pageStart, pageSize)
    }

    processCacheUpdate(cache, context, resourceDir)
    install(DefaultHeaders)
    install(CallLogging)
    install(HttpsRedirect) { sslPort = 443 }
    routing {
        get("/") { call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain) }
        get("/html-dsl") { call.respondHtml { getHtml(this) } }
        get("page/{listName}/{pageStart}/{pageSize}") { call.respondText(getIndexedPage(call.parameters)) }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") { resources("static") }
    }
}
