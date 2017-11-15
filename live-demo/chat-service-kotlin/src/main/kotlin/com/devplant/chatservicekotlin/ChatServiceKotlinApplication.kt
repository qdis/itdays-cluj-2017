package com.devplant.chatservicekotlin

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.CollectionOptions
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.data.mongodb.repository.Tailable
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import java.util.*

@SpringBootApplication
class ChatServiceKotlinApplication {

    @Bean
    fun mongoInit(mongoOperations: MongoOperations) = CommandLineRunner {
        mongoOperations.createCollection("message",
                CollectionOptions(1000000L, 5000L, true))
    }

    @Bean
    fun routes(messageRepo: MessageRepo) = router {
        GET("/chat", {
            ok().contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(messageRepo.findAllByDateAfter(Date()))
        })
        POST("/chat", {
            it.bodyToMono<Message>().flatMap {
                messageRepo.save(it.copy(date = Date()))
                        .then(ServerResponse.ok().build())
            }
        })
        GET("", { ok().contentType(MediaType.TEXT_HTML).render("index") })
    }

}

interface MessageRepo : ReactiveMongoRepository<Message, String> {

    @Tailable
    fun findAllByDateAfter(date: Date): Flux<Message>
}

@Document(collection = "message")
data class Message(var value: String, var from: String, var date: Date?)

fun main(args: Array<String>) {
    runApplication<ChatServiceKotlinApplication>(*args)
}