package com.decentri

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class TokenApp

fun main(args: Array<String>) {
    runApplication<TokenApp>(*args)
}