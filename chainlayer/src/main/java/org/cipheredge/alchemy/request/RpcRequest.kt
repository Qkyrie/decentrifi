package org.cipheredge.alchemy.request

data class RpcRequest<T : Any>(
    val method: String,
    val params: List<T>,
    val id: Long = 1L,
    val jsonRpc: String = "2.0"
)

fun rpc(method: String, vararg params: Any) = RpcRequest(method, params.toList())