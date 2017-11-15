package com.devplant.echobotkotlin

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux


@SpringBootApplication
class EchoBotKotlinApplication {

    @Bean
    fun webClient() = WebClient.create("http://localhost:8080/chat")

    @Bean
    fun echoBot(webClient: WebClient) = CommandLineRunner {

        webClient.get().retrieve().bodyToFlux<Message>().subscribe { message ->
            if (message.from != "EchoBot") {
                webClient.post().syncBody(
                        Message("EchoBot", "Echo Kotlin: ${message.value}"))
                        .exchange().subscribe()
            }
        }
    }


}


data class Message(var from: String, var value: String)


fun main(args: Array<String>) {
    runApplication<EchoBotKotlinApplication>(*args)
}

