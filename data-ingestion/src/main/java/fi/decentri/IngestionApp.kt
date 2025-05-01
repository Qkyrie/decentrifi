package fi.decentri

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IngestionApp

fun main(args: Array<String>) {
    runApplication<IngestionApp>(*args)
}