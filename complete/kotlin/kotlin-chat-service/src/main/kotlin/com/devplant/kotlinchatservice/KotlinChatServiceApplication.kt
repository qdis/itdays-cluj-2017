package com.devplant.kotlinchatservice

import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.http.MediaType
import org.springframework.integration.dsl.PublishSubscribeSpec
import org.springframework.integration.dsl.channel.MessageChannels
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.SubscribableChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@SpringBootApplication
class KotlinChatServiceApplication {

    @Bean
    fun router(chatHandler: ChatHandler) = router {
        "/".nest {
            GET("", { ServerResponse.ok().contentType(MediaType.TEXT_HTML).render("index") })
        }
        "/chat".nest {
            accept(MediaType.TEXT_EVENT_STREAM).nest {
                POST("", { request -> chatHandler.handlePost(request) })
                GET("", { request -> chatHandler.getStream(request) })
                GET("/stats", { request -> chatHandler.getStats(request) })
            }
        }
    }

    @Component
    class ChatHandler(val chatRepo: ChatRepo, val whitelist: Whitelist = Whitelist.basic(),
                      val messagesChannel: SubscribableChannel = MessageChannels.publishSubscribe<PublishSubscribeSpec>("messageChannel").get()) {

        fun getStream(serverRequest: ServerRequest): Mono<ServerResponse> {
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(Flux.create<ChatMessage> { sink ->
                        val messageHandler = MessageHandler { sink.next(it.payload as ChatMessage) }
                        sink.onCancel { messagesChannel.unsubscribe(messageHandler) }
                        messagesChannel.subscribe(messageHandler)
                    }, ChatMessage::class.java)
        }

        fun getStats(serverRequest: ServerRequest): Mono<ServerResponse> {
            return ServerResponse.ok().body(
                    chatRepo.findByFromAndValueStartingWith(serverRequest.queryParam("user").orElse(null),
                            serverRequest.queryParam("prefix").orElse(null)), ChatMessage::class.java)
        }

        fun handlePost(serverRequest: ServerRequest): Mono<ServerResponse> {
            return serverRequest.bodyToMono(ChatMessage::class.java).flatMap { chatMessage ->
                val message = chatMessage.copy(value = Jsoup.clean(chatMessage.value, whitelist), from = Jsoup.clean(chatMessage.from, whitelist), date = Date())
                if (!message.from.isBlank() && !message.value.isBlank()) {
                    chatRepo.save(message).flatMap({
                        messagesChannel.send(GenericMessage<Any>(message))
                        ServerResponse.ok().build()
                    })
                } else {
                    chatRepo.save(message).flatMap { ServerResponse.badRequest().build() }
                }
            }
        }


    }

}

fun main(args: Array<String>) {
    runApplication<KotlinChatServiceApplication>(*args)
}


interface ChatRepo : ReactiveMongoRepository<ChatMessage, String> {

    fun findByFromAndValueStartingWith(from: String, value: String): Flux<ChatMessage>

}


@Document(collection = "messages")
data class ChatMessage(var value: String, var from: String, var date: Date = Date())
