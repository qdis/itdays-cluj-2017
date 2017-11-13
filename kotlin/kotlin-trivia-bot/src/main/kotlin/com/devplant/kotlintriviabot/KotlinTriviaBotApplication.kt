package com.devplant.kotlintriviabot

import org.apache.commons.lang3.StringUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.WebClient
import java.util.*
import java.util.concurrent.atomic.AtomicLong

@SpringBootApplication
@EnableConfigurationProperties(TriviaProperties::class)
class KotlinTriviaBotApplication {

    @Bean
    internal fun initializer(triviaProperties: TriviaProperties) = CommandLineRunner {

        val skip = AtomicLong()
        val chatClient = WebClient.create("http://localhost:8082/chat")

        fun askQuestion(): Question {
            val question = triviaProperties.getRandomQuestion()
            chatClient.post().syncBody(ChatMessage(question.question, "TriviaBot")).exchange().subscribe()
            return question
        }

        var currentQuestion = askQuestion()

        chatClient.get().retrieve().bodyToFlux(ChatMessage::class.java).subscribe { message ->
            if (message.value.equals(currentQuestion.answer, ignoreCase = true)) {
                chatClient.post().syncBody(ChatMessage("Congratz: \"" + message.from + "\" You're right!", "TriviaBot"))
                        .exchange().subscribe { response ->
                    currentQuestion = askQuestion();

                }
            }

            if (message.value.equals("!t-info", ignoreCase = true)) {
                chatClient.post().syncBody(ChatMessage("Commands are: "
                        + "<br> * \"!t-repeat\" to repeat the last question"
                        + "<br> * \"!t-skip\" to skip the current question ( 5 skips are required )"
                        + "<br> * \"!t-stats\" to print stats for best trivia players", "TriviaBot")).exchange().subscribe()
            }

            if (message.value.equals("!t-skip", ignoreCase = true)) {
                if (skip.incrementAndGet() >= 5) {
                    askQuestion()
                }
            }

            if (message.value.equals("!t-repeat", ignoreCase = true)) {
                chatClient.post().syncBody(ChatMessage(currentQuestion.question, "TriviaBot")).exchange().subscribe()
            }

            if (message.value.equals("!t-stats", ignoreCase = true)) {
                chatClient.get().uri("/stats?user=TriviaBot&prefix=Congratz").retrieve().bodyToFlux(ChatMessage::class.java)
                        .collectList().map {
                    it.groupingBy { StringUtils.substringBetween(it.value, "\"", "\"") }
                            .eachCount()
                            .toList()
                            .sortedBy { (key, value) -> -value }
                            .take(3)
                            .fold(ChatMessage("Top 3 Users for Trivia:", "TriviaBot")) { message, pair ->
                                message.addValue("<br> * User: ${pair.first} scored: ${pair.second}")
                            }
                }.subscribe {
                    chatClient.post().syncBody(it).exchange().subscribe()
                }
            }
        }
    }


}

fun main(args: Array<String>) {
    runApplication<KotlinTriviaBotApplication>(*args)
}

data class ChatMessage(var value: String, var from: String) {

    fun addValue(value: String): ChatMessage {
        this.value += value
        return this
    }

}

@ConfigurationProperties("trivia")
data class TriviaProperties(var questions: List<Question> = arrayListOf(), val random: Random = Random()) {

    fun getRandomQuestion(): Question {
        return questions[random.nextInt(questions.size)]
    }

}

data class Question(var question: String = "", var answer: String = "")
