package fi.decentri.application.ports

import fi.decentri.infrastructure.abi.AbiEvent
import fi.decentri.infrastructure.abi.AbiFunction

interface AbiPort {
    fun parseABI(abiString: String): Pair<List<AbiFunction>, List<AbiEvent>>
}