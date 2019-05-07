package com.pajato.tmdb.server

import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.zip.GZIPInputStream

internal fun getResourceFileForList(listName: String, context: FetchContext, resourceDir: File): File {
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

/** Uncompress and save a gzip compressed file ending in .gz using the base name and removing the .gz file. */
internal fun File.uncompress(): File =
    File(this.parentFile, this.nameWithoutExtension).apply {
        GZIPInputStream(this@uncompress.inputStream()).apply { copyTo(outputStream()) }
        this@uncompress.inputStream().close()
        this@uncompress.delete()
    }
