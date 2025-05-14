package fi.decentri.dataapi.k8s.ingestion

import fi.decentri.dataapi.model.Contract
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import kotlin.time.ExperimentalTime

@ExperimentalTime
class IngestionLauncher(
    private val namespace: String = "decentrifi",
    clientProvider: () -> io.fabric8.kubernetes.client.KubernetesClient = { KubernetesClientBuilder().build() }
) {
    private val client = clientProvider()

    fun launchManualRun(contract: Contract): String {
        val cj = client.batch().v1().cronjobs()
            .inNamespace(namespace)
            .withName("data-ingestion")
            .get() ?: error("CronJob not found")

        // Clone the template and inject args
        val jobSpec = cj.spec.jobTemplate.spec
        val container = jobSpec.template.spec.containers[0]      // assume one container

        // Build the args list - add type if provided
        val args = mutableListOf("--mode=contract", "--contract", contract.address, "--network", contract.chain)
        container.args = args

        val job = JobBuilder()
            .withNewMetadata()
            .withGenerateName("ingestion-manual-")
            .withNamespace(namespace)
            .endMetadata()
            .withSpec(jobSpec)
            .build()

        val created = client.batch().v1().jobs()
            .inNamespace(namespace)
            .resource(job)
            .create()

        return created.metadata.name
    }
}