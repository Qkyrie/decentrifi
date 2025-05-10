package fi.decentri.dataapi.k8s.ingestion

import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder

class IngestionLauncher(
    private val namespace: String = "decentrifi",
    clientProvider: () -> io.fabric8.kubernetes.client.KubernetesClient = { KubernetesClientBuilder().build() }
) {
    private val client = clientProvider()

    fun launchManualRun(contract: String): String {
        val cj = client.batch().v1().cronjobs()
            .inNamespace(namespace)
            .withName("ingestion")
            .get() ?: error("CronJob not found")

        // Clone the template and inject args
        val jobSpec = cj.spec.jobTemplate.spec
        val container = jobSpec.template.spec.containers[0]      // assume one container
        container.args = listOf("--contract", contract)

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