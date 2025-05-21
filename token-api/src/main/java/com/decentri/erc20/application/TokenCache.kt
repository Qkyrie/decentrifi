package com.decentri.erc20.application

import arrow.core.Option
import com.decentri.erc20.domain.FungibleToken
import fi.decentri.evm.Chain
import io.ktor.util.collections.*
import org.springframework.stereotype.Component

typealias Cache = ConcurrentMap<String, Option<FungibleToken>>


@Component
class TokenCache {

    val cache: Cache = Cache()

    private fun createIndex(address: String, network: Chain): String {
        return "${address.lowercase()}-$network"
    }

    fun put(address: String, network: Chain, fungibleToken: Option<FungibleToken>): Option<FungibleToken> {
        cache[createIndex(address, network)] = fungibleToken
        return fungibleToken
    }

    fun get(address: String, network: Chain): Option<FungibleToken>? {
        return cache[createIndex(address, network)]
    }

    fun getAll(): HashMap<String, Option<FungibleToken>> {
        return HashMap(cache)
    }

    fun find(network: Chain, verified: Boolean): List<FungibleToken> =
        HashMap(cache).asSequence().filter {
            it.value.isSome()
        }.filter {
            network == it.value.getOrNull()?.network
        }.mapNotNull {
            it.value.getOrNull()
        }.distinctBy {
            it.address.lowercase() + "-" + it.network.name
        }.filter {
            !verified || (it.verified == verified)
        }.toList()
}