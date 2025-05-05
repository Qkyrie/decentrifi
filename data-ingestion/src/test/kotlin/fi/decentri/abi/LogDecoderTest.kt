package fi.decentri.abi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.decentri.abi.LogDecoder.decodeLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.web3j.protocol.core.methods.response.Log
import java.math.BigInteger
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DecodeLogTest {


    @DisplayName("decodeLog should correctly map log topics/data to parameter map")
    @ParameterizedTest(name = "#{index} – {0}")
    @MethodSource("arguments")
    fun testDecodeLog(
        caseName: String,
        abi: String,
        log: Log,
        expected: Map<String, Any?>
    ) {
        val actual = decodeLog(log, abi)
        assertEquals(expected, actual, "Mismatch for $caseName")
    }

    /** ----------  Test‑case generator  ---------- */
    fun arguments(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
        /* ERC‑20 Transfer --------------------------------------------------- */
        org.junit.jupiter.params.provider.Arguments.of(
            "ERC20‑Transfer",
            """
            [
              {
                "anonymous": false,
                "inputs": [
                  {"indexed": true,  "name": "from",  "type": "address"},
                  {"indexed": true,  "name": "to",    "type": "address"},
                  {"indexed": false, "name": "value", "type": "uint256"}
                ],
                "name": "Transfer",
                "type": "event"
              }
            ]
            """.trimIndent(),
            Log().apply {
                address = "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"     // USDC
                topics = listOf(
                    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55aef664d03c4",                     // keccak256("Transfer(address,address,uint256)")
                    "0x0000000000000000000000001111111111111111111111111111111111111111",                     // from
                    "0x0000000000000000000000002222222222222222222222222222222222222222"                      // to
                )
                // value = 1_000_000 USDC (6 decimals)
                data = "0x" + BigInteger("1000000").toString(16).padStart(64, '0')
            },
            mapOf(
                "from"  to "0x1111111111111111111111111111111111111111",
                "to"    to "0x2222222222222222222222222222222222222222",
                "value" to BigInteger("1000000")
            )
        ),

        /* BoolChanged(bool) ------------------------------------------------- */
        org.junit.jupiter.params.provider.Arguments.of(
            "BoolChanged",
            """
            [
              {
                "anonymous": false,
                "inputs": [
                  {"indexed": false, "name": "newValue", "type": "bool"}
                ],
                "name": "BoolChanged",
                "type": "event"
              }
            ]
            """.trimIndent(),
            Log().apply {
                address = "0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"
                topics = listOf(
                    "0xee459af3dd4e87008e83820c113fa49458a53993627e07defc07244e6fb97066" // keccak256("BoolChanged(bool)")
                )
                // bool true => 1 in 32‑byte left‑padded hex
                data = "0x" + "0".repeat(63) + "1"
            },
            mapOf("newValue" to true)
        )
    )
}
