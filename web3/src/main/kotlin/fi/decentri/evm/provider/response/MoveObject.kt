package fi.decentri.evm.provider.response

import com.fasterxml.jackson.databind.JsonNode

data class MoveObject(
    val objectId: String,
    val content: MoveObjectContent
)

data class MoveObjectContent(
    val fields: JsonNode
)
