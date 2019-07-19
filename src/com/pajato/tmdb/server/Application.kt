package com.pajato.tmdb.server

import com.typesafe.config.ConfigFactory
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.HoconApplicationConfig
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.HttpsRedirect
import io.ktor.http.Parameters
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import java.io.File
import java.nio.file.Paths
import kotlin.math.min

/** See Ktor documentation for Application ... */
@KtorExperimentalAPI
fun main(args: Array<String>) {
    println("Running main...")
    generateCertificate()
    io.ktor.server.netty.EngineMain.main(args)
}

@KtorExperimentalAPI
internal fun generateCertificate() {
    val config = HoconApplicationConfig(ConfigFactory.load())
    val path = config.property("ktor.security.ssl.keyStore").getString()
    if (path != "build/keystore.jks" || File(path).exists()) return
    val file = File(path)
    val alias = config.property("ktor.security.ssl.keyAlias").getString()
    val keyStorePassword = config.property("ktor.security.ssl.keyStorePassword").getString()
    val privateKeyPassword = config.property("ktor.security.ssl.privateKeyPassword").getString()

    file.parentFile.mkdirs()
    generateCertificate(file, keyAlias = alias, jksPassword = keyStorePassword, keyPassword = privateKeyPassword)
}

@Suppress("unused") // Referenced in the resource file: application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false, context: FetchContext = ContextImpl()) {
    val welcomeText = "Welcome to the Pajato TMDB(tm) dataset server!"
    val resourceDir by lazy { Paths.get(this::class.java.classLoader.getResource("datasets").toURI()).toFile() }
    val cache: MutableMap<String, List<String>> = mutableMapOf()
    fun installFeatures() {
        install(DefaultHeaders)
        install(CallLogging)
        if (!testing) install(HttpsRedirect) { sslPort = 443 }
    }
    fun setupRouting() {
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
                    val endIndex = min(list.size - 1, startIndex + pageSize - 1)

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

        routing {
            get( "/") { call.respondText(welcomeText) }
            get("page/{listName}/{pageStart}/{pageSize}") { call.respondText(getIndexedPage(call.parameters)) }
        }
    }

    if (File("build/keystore.jks").exists() || testing) println("Using a test server!")
    processCacheUpdate(cache, context, resourceDir)
    installFeatures()
    setupRouting()
}
