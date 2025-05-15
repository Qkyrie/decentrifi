package org.cipheredge.chain.evm.web3j

import org.cipheredge.chain.evm.web3j.domain.RunWithFallbackContext
import org.cipheredge.rest.request.EvmContractInteractionRequest
import org.cipheredge.rest.request.GetEventLogsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.*
import java.math.BigInteger
import java.util.regex.Matcher
import java.util.regex.Pattern

@Component
class Web3JProxyImpl : Web3jProxy {


    override suspend fun getLogs(
        getEventLogsRequest: GetEventLogsRequest,
        _web3j: Web3j
    ): EthLog {
        val ethFilter = getEthFilter(getEventLogsRequest)

        val log = runWithRetry(
            RunWithFallbackContext {
                _web3j.ethGetLogs(ethFilter)
            }
        )

        return when {
            log.hasError() -> {
                val exceededMatcher = exceededPattern.matcher(log.error.message ?: "")
                if (exceededMatcher.find() && getEventLogsRequest.fetchAll == true) {
                    return fromSplitMatcher(exceededMatcher, getEventLogsRequest, _web3j)
                } else if (log.exceedsBlockLimit()) {
                    split(getEventLogsRequest, _web3j)
                } else {
                    log
                }
            }

            else -> {
                log
            }
        }
    }

    private suspend fun split(getEventLogsRequest: GetEventLogsRequest, _web3j: Web3j): EthLog {
        if (getEventLogsRequest.fromBlock == BigInteger.ZERO || getEventLogsRequest.fromBlock == null) {
            val currentBlock =
                _web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().block.number
            val start = currentBlock.minus(BigInteger.valueOf(9999))
            return getLogs(
                GetEventLogsRequest(
                    getEventLogsRequest.addresses,
                    getEventLogsRequest.topic,
                    getEventLogsRequest.optionalTopics,
                    start,
                    currentBlock
                ),
                _web3j
            )
        }
        val startBlock = getEventLogsRequest.fromBlock ?: BigInteger.ZERO
        val endBlock = startBlock + BigInteger.valueOf(10000)
        logger.info("too many results, splitting into two calls: {} - {}", startBlock, endBlock)
        return split(getEventLogsRequest, startBlock, endBlock, _web3j)
    }

    suspend fun split(
        request: GetEventLogsRequest,
        startBlock: BigInteger,
        endBlock: BigInteger,
        _web3j: Web3j
    ): EthLog {
        return listOf(
            getLogs(
                GetEventLogsRequest(
                    request.addresses,
                    request.topic,
                    request.optionalTopics,
                    startBlock,
                    endBlock
                ),
                _web3j
            ),
            getLogs(
                GetEventLogsRequest(
                    request.addresses,
                    request.topic,
                    request.optionalTopics,
                    endBlock.plus(BigInteger.ONE),
                    request.toBlock
                ),
                _web3j
            )
        ).reduce { acc, ethLog ->
            return EthLog().apply {
                this.result = (acc.logs ?: emptyList()) + (ethLog.logs ?: emptyList())
                this.error = if (acc.hasError()) acc.error else ethLog.error
            }
        }
    }

    private suspend fun fromSplitMatcher(
        matcher: Matcher,
        getEventLogsRequest: GetEventLogsRequest,
        _web3j: Web3j
    ): EthLog {
        val start = matcher.group(1).removePrefix("0x")
        val end = matcher.group(2).removePrefix("0x")
        val startBlock = BigInteger(start, 16)
        val endBlock = BigInteger(end, 16)
        logger.info("too many results, splitting into two calls: {} - {}", startBlock, endBlock)
        return split(getEventLogsRequest, startBlock, endBlock, _web3j)
    }

    private fun getEthFilter(getEventLogsRequest: GetEventLogsRequest): EthFilter {
        return with(
            EthFilter(
                getEventLogsRequest.fromBlock?.let {
                    DefaultBlockParameterNumber(it)
                } ?: DefaultBlockParameterName.EARLIEST,
                getEventLogsRequest.toBlock?.let {
                    DefaultBlockParameterNumber(it)
                } ?: DefaultBlockParameterName.LATEST,
                getEventLogsRequest.addresses
            )
        ) {
            addSingleTopic(getEventLogsRequest.topic)
            getEventLogsRequest.optionalTopics.forEach {
                if (it != null) {
                    addOptionalTopics(it)
                } else {
                    addNullTopic()
                }
            }
            this
        }
    }

    val exceededPattern: Pattern
        get() {
            val regex = "[\\s\\S]*?block range should work:\\s*\\[(0x[0-9a-fA-F]+),\\s*(0x[0-9a-fA-F]+)\\]"
            return Pattern.compile(regex)
        }

    fun EthLog.exceedsBlockLimit(): Boolean {
        return this.hasError() && this.error.message.contains("logs are limited to a 10000 block range")
    }

    override suspend fun getBlockByHash(hash: String, _web3j: Web3j): EthBlock? {
        return runWithRetry(
            RunWithFallbackContext {
                _web3j.ethGetBlockByHash(hash, false)
            }
        )
    }

    override suspend fun ethGetBalance(
        address: String,
        _web3j: Web3j
    ): EthGetBalance {
        return runWithRetry(
            RunWithFallbackContext {
                _web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST)
            }
        )
    }

    override suspend fun getTransactionByHash(
        txId: String,
        _web3j: Web3j
    ): EthTransaction {
        return runWithRetry(
            RunWithFallbackContext {
                _web3j.ethGetTransactionByHash(txId)
            }
        )
    }


    override suspend fun getTransactionReceipt(
        txId: String,
        _web3j: Web3j
    ): EthGetTransactionReceipt {
        return runWithRetry(
            RunWithFallbackContext {
                _web3j.ethGetTransactionReceipt(txId)
            }
        )
    }

    override suspend fun call(
        evmContractInteractionRequest: EvmContractInteractionRequest,
        _web3j: Web3j
    ): EthCall {
        return runWithRetry(
            RunWithFallbackContext {
                _web3j.ethCall(
                    Transaction.createEthCallTransaction(
                        evmContractInteractionRequest.from,
                        evmContractInteractionRequest.contract,
                        evmContractInteractionRequest.function
                    ), evmContractInteractionRequest.block?.let {
                        DefaultBlockParameterNumber(it)
                    } ?: DefaultBlockParameterName.PENDING
                )
            }
        )
    }
}

private val logger = LoggerFactory.getLogger(Web3JProxyImpl::class.java)

suspend fun <T : Response<*>> runWithRetry(previousContext: RunWithFallbackContext<T>): T {
    val runWithFallbackContext = previousContext.increment()
    val request = runWithFallbackContext.requestProvider.invoke()
    val result = withContext(Dispatchers.IO) {
        request.send()
    }

    return if (result.hasError()) {
        when {
            result.error.message.contains("429") -> {
                logger.info("throttled, waiting and running with fallback")
                delay(100L)
                runWithRetry(runWithFallbackContext)
            }

            result.error.message.contains("limit exceeded") -> {
                logger.info("capacity exceeded")
                throw RuntimeException("capacity exceeded")
            }

            else -> {
                result
            }
        }
    } else {
        result
    }
}