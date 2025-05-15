package org.cipheredge.chain.evm.web3j

import org.cipheredge.rest.request.EvmContractInteractionRequest
import org.cipheredge.rest.request.GetEventLogsRequest
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.*

interface Web3jProxy {
    suspend fun getLogs(getEventLogsRequest: GetEventLogsRequest, _web3j: Web3j): EthLog
    suspend fun getBlockByHash(hash: String, _web3j: Web3j): EthBlock?
    suspend fun ethGetBalance(address: String, _web3j: Web3j): EthGetBalance
    suspend fun getTransactionByHash(txId: String, _web3j: Web3j): EthTransaction
    suspend fun getTransactionReceipt(txId: String, _web3j: Web3j): EthGetTransactionReceipt
    suspend fun call(evmContractInteractionRequest: EvmContractInteractionRequest, _web3j: Web3j): EthCall
}