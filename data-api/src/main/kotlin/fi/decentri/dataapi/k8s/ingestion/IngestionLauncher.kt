package fi.decentri.dataapi.k8s.ingestion

import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder

class IngestionLauncher(
    private val namespace: String = "decentrifi",
    clientProvider: () -> io.fabric8.kubernetes.client.KubernetesClient = { KubernetesClientBuilder().build() }
) {
    private val client = clientProvider()

    fun launchManualRun(): String {
        // 1) Fetch the existing CronJob to reuse its Job template
        val cj = client.batch().v1().cronjobs()
            .inNamespace(namespace)
            .withName("data-ingestion")
            .get() ?: error("CronJob 'ingestion' not found")

        // 2) Build a one‑off Job
        val job = JobBuilder()
            .withNewMetadata()
                .withGenerateName("ingestion-manual-")         // server will append a random suffix
                .withNamespace(namespace)
            .endMetadata()
            .withSpec(cj.spec.jobTemplate.spec)                // re‑use template exactly
            .build()

        // 3) Create it
        val created = client.batch().v1().jobs()
            .inNamespace(namespace)
            .resource(job)
            .create()

        return created.metadata.name // return Job’s final name for the caller / logs
    }
}