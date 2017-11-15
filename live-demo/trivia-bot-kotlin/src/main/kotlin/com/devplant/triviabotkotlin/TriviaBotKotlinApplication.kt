package com.devplant.triviabotkotlin

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import java.util.*

@SpringBootApplication
class TriviaBotKotlinApplication {

    @Bean
    fun webClient() = WebClient.create("http://localhost:8080/chat")

    @Bean
    fun initializer(chatClient: WebClient) = CommandLineRunner {

        fun askQuestion(): Question {
            val currentQuestion = randomQuestion()
            chatClient.post().syncBody(ChatMessage(currentQuestion.question, "TriviaBot")).exchange().subscribe()
            return currentQuestion
        }


        var currentQuestion = askQuestion()
        chatClient.get().retrieve().bodyToFlux<ChatMessage>().subscribe { message ->
            if (message.value.toLowerCase() == currentQuestion.answer.toLowerCase()) {
                chatClient.post().syncBody(ChatMessage(
                        "Congratz: \"" + message.from + "\" You're right!", "TriviaBot"))
                        .exchange().subscribe { currentQuestion = askQuestion() }
            }
        }


    }
}

/**
 * Our Chat Message class
 */
data class ChatMessage(var value: String, var from: String)

/**
 * Some Pre-defined Trivia Questions
 */
data class Question(var question: String = "", var answer: String = "")

val questions: List<Question> = arrayListOf(
        Question("Who Wrote this code?", "Timo"),
        Question("What is the meaning of life?", "42"),
        Question("Pirates VS Ninjas, who would win?", "Ninjas")
)

fun randomQuestion(): Question {
    return questions[Random().nextInt(questions.size)]
}

fun main(args: Array<String>) {
    runApplication<TriviaBotKotlinApplication>(*args)
}


