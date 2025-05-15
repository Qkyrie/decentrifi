package fi.decentri.dataapi.service

class TokenService {

    val tokens = mapOf(
        "ethereum" to listOf(
            Token(
                "USDC", "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48", 6
            ),
            Token(
                "USDT", "0xdac17f958d2ee523a2206206994597c13d831ec7", 6
            )
        )
    )

    fun getToken(chain: String, address: String): Token? {
        return tokens[chain.lowercase()]?.firstOrNull {
            it.address == address.lowercase()
        }
    }

    data class Token(
        val name: String,
        val address: String,
        val decimals: Int
    )

}