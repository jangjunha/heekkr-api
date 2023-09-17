package kr.heek.api.book

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.completeWith
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RequestContext<K, V>(
    private val resolver: suspend (K) -> V,
) {
    private val cache: MutableMap<K, CompletableDeferred<V>> = mutableMapOf()
    private val mutex = Mutex()

    suspend fun request(key: K): V {
        val (exists, deferred) = mutex.withLock {
            cache[key]?.let { true to it } ?: (false to CompletableDeferred<V>().also {
                cache[key] = it
            })
        }

        if (!exists) {
            val result = runCatching { resolver(key) }
            deferred.completeWith(result)
        }

        return deferred.await()
    }
}
