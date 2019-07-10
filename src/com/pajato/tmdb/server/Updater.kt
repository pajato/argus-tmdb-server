package com.pajato.tmdb.server

import com.soywiz.klock.DateTime.Companion.now
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

internal fun processCacheUpdate(
    cache: MutableMap<String, List<String>>,
    context: FetchContext = ContextImpl(),
    resourceDir: File) {
    GlobalScope.launch(Dispatchers.Default) {
        fun clearState() {
            // Clear out any stale dataset files.
            resourceDir.listFiles().forEach { if (it.isFile) it.delete() }
            cache.clear()
        }

        do {
            clearState()
            val nextTime = context.startTime + context.updateIntervalMillis
            val delayInMillis = nextTime - now().unixMillisLong
            if (delayInMillis > 0) delay(delayInMillis)
        } while (context.updateAction())
    }
}
