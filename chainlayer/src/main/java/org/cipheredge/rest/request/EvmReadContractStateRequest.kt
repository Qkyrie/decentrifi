package org.cipheredge.rest.request

import org.cipheredge.rest.request.input.Input
import org.cipheredge.rest.request.output.Output

class EvmReadContractStateRequest(
    val contract: String,
    val method: String,
    val inputs: List<Input<*>>,
    val outputs: List<Output>
)



