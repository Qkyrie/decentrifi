package fi.decentri.dataingest

import fi.decentri.abi.AbiService
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AbiServiceTest {

    private val abiService = AbiService()
    
    @Test
    fun `test parseABI with ERC20 ABI`() {
        // ERC-20 ABI sample (simplified)
        val erc20AbiJson = """
        [
            {
                "constant": true,
                "inputs": [],
                "name": "name",
                "outputs": [{"name": "", "type": "string"}],
                "payable": false,
                "stateMutability": "view",
                "type": "function"
            },
            {
                "constant": true,
                "inputs": [],
                "name": "symbol",
                "outputs": [{"name": "", "type": "string"}],
                "payable": false,
                "stateMutability": "view",
                "type": "function"
            },
            {
                "constant": true,
                "inputs": [],
                "name": "decimals",
                "outputs": [{"name": "", "type": "uint8"}],
                "payable": false,
                "stateMutability": "view",
                "type": "function"
            },
            {
                "constant": false,
                "inputs": [
                    {"name": "_to", "type": "address"},
                    {"name": "_value", "type": "uint256"}
                ],
                "name": "transfer",
                "outputs": [{"name": "", "type": "bool"}],
                "payable": false,
                "stateMutability": "nonpayable",
                "type": "function"
            },
            {
                "anonymous": false,
                "inputs": [
                    {"indexed": true, "name": "from", "type": "address"},
                    {"indexed": true, "name": "to", "type": "address"},
                    {"indexed": false, "name": "value", "type": "uint256"}
                ],
                "name": "Transfer",
                "type": "event"
            },
            {
                "anonymous": false,
                "inputs": [
                    {"indexed": true, "name": "owner", "type": "address"},
                    {"indexed": true, "name": "spender", "type": "address"},
                    {"indexed": false, "name": "value", "type": "uint256"}
                ],
                "name": "Approval",
                "type": "event"
            }
        ]
        """.trimIndent()
        
        // Parse the ABI
        val (functions, events) = abiService.parseABI(erc20AbiJson)
        
        // Verify functions
        assertEquals(4, functions.size, "Should find 4 functions")
        
        // Check name function
        val nameFunction = functions.find { it.name == "name" }
        assertNotNull(nameFunction, "Should find 'name' function")
        assertEquals(0, nameFunction.inputs.size, "Name function should have no inputs")
        assertEquals(1, nameFunction.outputs.size, "Name function should have one output")
        assertEquals("view", nameFunction.stateMutability, "Name function should be view")
        
        // Check transfer function
        val transferFunction = functions.find { it.name == "transfer" }
        assertNotNull(transferFunction, "Should find 'transfer' function")
        assertEquals(2, transferFunction.inputs.size, "Transfer function should have 2 inputs")
        assertEquals("_to", transferFunction.inputs[0].name, "First input should be '_to'")
        assertEquals("address", transferFunction.inputs[0].type, "First input should be of type 'address'")
        assertEquals("_value", transferFunction.inputs[1].name, "Second input should be '_value'")
        assertEquals("uint256", transferFunction.inputs[1].type, "Second input should be of type 'uint256'")
        
        // Verify events
        assertEquals(2, events.size, "Should find 2 events")
        
        // Check Transfer event
        val transferEvent = events.find { it.name == "Transfer" }
        assertNotNull(transferEvent, "Should find 'Transfer' event")
        assertEquals(3, transferEvent.inputs.size, "Transfer event should have 3 inputs")
        assertEquals("from", transferEvent.inputs[0].name, "First input should be 'from'")
        assertTrue(transferEvent.inputs[0].indexed, "First input should be indexed")
        assertEquals("to", transferEvent.inputs[1].name, "Second input should be 'to'")
        assertTrue(transferEvent.inputs[1].indexed, "Second input should be indexed")
        assertEquals("value", transferEvent.inputs[2].name, "Third input should be 'value'")
        assertTrue(!transferEvent.inputs[2].indexed, "Third input should not be indexed")

        // Check Approval event
        val approvalEvent = events.find { it.name == "Approval" }
        assertNotNull(approvalEvent, "Should find 'Approval' event")
        assertEquals(3, approvalEvent.inputs.size, "Approval event should have 3 inputs")
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `test parseABI with invalid JSON`() {
        val invalidJson = "{ invalid json }"
        abiService.parseABI(invalidJson)
        // Should throw IllegalArgumentException
    }
}
