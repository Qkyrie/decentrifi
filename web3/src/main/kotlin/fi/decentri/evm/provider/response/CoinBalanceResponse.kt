package fi.decentri.evm.provider.response

import java.math.BigInteger

data class CoinBalanceResponse(
    val coinType: String,
    val balance: BigInteger
)