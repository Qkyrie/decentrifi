package org.cipheredge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan("org.cipheredge")
class ChainlayerApp

fun main(args: Array<String>) {
    runApplication<ChainlayerApp>(*args)
}