package fi.decentri.application.ports

import fi.decentri.dataingest.model.MetadataType

interface IngestionMetadataPort {
    suspend fun getMetadatForContractId(type: MetadataType, contractId: Int): String?
    suspend fun updateMetadataForContractId(contractId: Int, type: MetadataType, theValue: String)
}