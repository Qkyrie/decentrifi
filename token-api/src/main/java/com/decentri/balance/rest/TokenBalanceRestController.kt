package com.decentri.balance.rest

import arrow.core.*
import arrow.core.Either.Companion.catch
import arrow.fx.coroutines.parMap
import com.decentri.balance.adapter.TokenBalanceVO
import com.decentri.balance.port.TokenBalances
import com.decentri.balance.rest.vo.BalanceElementVO
import com.decentri.balance.rest.vo.TokenBalancesVO
import com.decentri.balance.rest.vo.noBalance
import com.decentri.erc20.application.TokenInformationService
import com.decentri.erc20.domain.toVO
import fi.decentri.evm.Chain
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*
import java.math.BigInteger

@RestController
@RequestMapping("/balance")
class TokenBalanceRestController(
    private val tokenBalances: TokenBalances,
    private val tokenService: TokenInformationService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/{address}")
    fun getAllTokenBalances(@PathVariable("address") address: String): List<BalanceElementVO> {
        return runBlocking {
            Chain.entries.parMap(concurrency = 8) {
                tokenBalances.getTokenBalances(it, address).map {
                    it.toBalanceElement()
                } + run {
                    getNativeBalance(address, it)
                        .map(BalanceElementVO::nel)
                        .getOrElse { emptyList() }
                }
            }.flatten()
        }
    }

    @GetMapping("/{address}/{network}")
    fun getTokenBalancesForNetwork(
        @PathVariable("address") address: String,
        @PathVariable("network") network: String
    ): Any = runBlocking {
        val n = Chain.fromStringOrNull(network) ?: throw IllegalArgumentException("Invalid network $network")

        if (n.addressValidator.isValid(address).not()) {
            logger.info("Invalid address $address for network $network, returnin no balances")
            return@runBlocking noBalance()
        }

        catch {
            TokenBalancesVO(
                tokenBalances.getTokenBalances(n, address).map {
                    it.toBalanceElement()
                } + run {
                    getNativeBalance(address, n)
                        .map { it.nel() }
                        .getOrElse { emptyList() }
                }.filter {
                    it.dollarValue > 1.0
                }
            )
        }.mapLeft {
            logger.error("Unable to fetch balances for $address on $network", it)
            noBalance()
        }.getOrElse {
            noBalance()
        }
    }

    suspend fun TokenBalanceVO.toBalanceElement(): BalanceElementVO {

        return BalanceElementVO(
            amount = amount,
            network = network.slug,
            token = token,
            price = 0.0
        )
    }

    suspend fun getNativeBalance(address: String, network: Chain): Option<BalanceElementVO> {
        return catch {
            val balance = tokenBalances.getNativeBalance(address, network)

            if (balance > BigInteger.ZERO) {
                BalanceElementVO(
                    amount = balance,
                    network = network.slug,
                    token = tokenService.getTokenInformation("0x0", network).getOrElse {
                        throw IllegalArgumentException("unable to get native token for $network")
                    }.toVO(),
                    price = 0.0
                ).some()
            } else {
                none()
            }
        }.getOrElse {
            logger.error("Unable to fetch balance for $network", it)
            none()
        }
    }
}