package org.cipheredge.rest

import org.cipheredge.rest.request.output.Output
import org.cipheredge.chain.EthCallResultToTypeConverter
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/tuple-decoder")
class TupleDecoderRestController(private val ethCallResultToTypeConverter: EthCallResultToTypeConverter) {

    @PostMapping
    fun decodeTuple(@RequestBody decodeTupleRequest: DecodeTupleRequest): List<EthCallResultToTypeConverter.Result> {
        return ethCallResultToTypeConverter.convert(
            decodeTupleRequest.outputs.map {
                it.makeTypeReference()
            },
            decodeTupleRequest.data,
        )
    }


    data class DecodeTupleRequest(
        val data: String,
        val outputs: List<Output>
    )
}