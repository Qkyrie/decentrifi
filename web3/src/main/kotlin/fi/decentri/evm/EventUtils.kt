package fi.decentri.evm

import org.web3j.abi.EventEncoder
import org.web3j.abi.FunctionReturnDecoder
import org.web3j.abi.datatypes.Event
import org.web3j.protocol.core.methods.response.Log

abstract class EventUtils {

    companion object {
        inline fun <reified T> Event.extract(log: Log, indexed: Boolean, index: Int): T {
            return if (indexed) {
                getIndexedParameter(log, index)
            } else {
                getNonIndexedParameter(log, index)
            }
        }

        inline fun <reified T> Event.getNonIndexedParameter(log: Log, index: Int): T {
            return FunctionReturnDecoder.decode(
                log.data,
                nonIndexedParameters
            )[index].value as T
        }

        inline fun <reified T> Event.getIndexedParameter(log: Log, index: Int): T {
            return FunctionReturnDecoder.decodeIndexedValue(
                log.topics[index + 1], indexedParameters[index]
            ).value as T
        }

        fun Event.getEncodedTopic(): String {
            return EventEncoder.encode(this)
        }
    }
}