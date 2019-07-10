package com.pajato.tmdb.server

import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTime.Companion.now
import com.soywiz.klock.hours
import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.zip.GZIPInputStream

// The TMDB dataset fetch context to differentiate production vs testing access.
abstract class FetchContext {
    abstract val cycles: Int
    abstract val baseUrl: String
    abstract val readTimeoutMillis: Int
    abstract val connectTimeoutMillis: Int
    abstract val updateIntervalMillis: Long
    abstract var startTime: Long
    abstract var exportDate: String

    private var counter = 0

    internal fun updateAction(): Boolean {
        startTime += updateIntervalMillis
        if (cycles == -1) exportDate = getLastExportDate(DateTime(startTime))
        counter++
        return if (cycles == -1) true else (counter < cycles)
    }
}

// The default context for managing dataset fetches from TMDB.
data class ContextImpl(
    override val cycles: Int = -1,
    override var startTime: Long = now().unixMillisLong,
    override var exportDate: String = getLastExportDate(DateTime(startTime)),
    override val baseUrl: String = "https://files.tmdb.org/p/exports/",
    override val readTimeoutMillis: Int = 800,
    override val connectTimeoutMillis: Int = 200,
    override val updateIntervalMillis: Long = 24L * 60 * 60 * 1000
) : FetchContext()

internal fun getLastExportDate(date: DateTime): String =
    // If the time is after 8:00am UTC, use today's datasets, otherwise use yesterday's.
    if (date.isAfter(8)) date.toTmdbFormat() else (date - 24.hours).toTmdbFormat()

internal fun getResourceFileForList(listName: String, context: FetchContext = ContextImpl(), resourceDir: File): File {
    val baseNmae = "${listName}_${context.exportDate}.json"
    val outFile = File(resourceDir, "$baseNmae.gz")
    fun copyFileFromConnection(connection: URLConnection): File {
        fun copyStreamToResource(stream: InputStream): File {
            stream.copyTo(outFile.outputStream())
            stream.close()
            return outFile.uncompress()
        }

        connection.readTimeout = context.readTimeoutMillis
        connection.connectTimeout = context.connectTimeoutMillis
        return copyStreamToResource(connection.getInputStream())
    }
    fun getErrorFile(exc: Exception) = File("No such file caused by exception: ${exc.message}!")

    val url = "${context.baseUrl}$baseNmae.gz"
    return try { copyFileFromConnection(URL(url).openConnection()) } catch (exc: Exception) { getErrorFile(exc) }
}

// Extension functions

internal fun Int.toTmdbFormat() = if (this > 9) "$this" else "0$this"
internal fun DateTime.toTmdbFormat() = "${this.month1.toTmdbFormat()}_${this.dayOfMonth.toTmdbFormat()}_${this.yearInt}"
internal fun DateTime.isAfter(time: Int): Boolean = this.hours > time

/** Uncompress and save a gzip compressed file ending in .gz using the base name and removing the .gz file. */
internal fun File.uncompress(): File =
    File(this.parentFile, this.nameWithoutExtension).apply {
        GZIPInputStream(this@uncompress.inputStream()).apply { copyTo(outputStream()) }
        this@uncompress.inputStream().close()
        this@uncompress.delete()
    }
