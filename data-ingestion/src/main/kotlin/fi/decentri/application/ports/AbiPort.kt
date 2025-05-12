package fi.decentri.application.ports

import fi.decentri.abi.AbiEvent
import fi.decentri.abi.AbiFunction

interface AbiPort {
    fun parseABI(abiString: String): Pair<List<AbiFunction>, List<AbiEvent>>
}