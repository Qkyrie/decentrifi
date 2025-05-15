package fi.decentri.evm.provider.response

import com.fasterxml.jackson.annotation.JsonProperty

data class CoinMetadata(
    val decimals: Int,
    val name: String,
    val symbol: String,
    @JsonProperty("iconUrl")
    val icon: String,
    val id: String
)