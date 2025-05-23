package fi.decentri.evm

import kotlinx.coroutines.*

object AsyncUtils {

    suspend fun <T> Deferred<T>.await(timeout: Long, defaultValue: T) =
        withTimeoutOrNull(timeout) { await() } ?: defaultValue

    fun <T> lazyAsync(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return GlobalScope.async(Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
            block()
        }
    }
}