package com.devplant.triviabot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(TriviaBotApplication.TriviaProperties.class)
public class TriviaBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TriviaBotApplication.class, args);
    }


    @Service
    class QuestionsAsker implements CommandLineRunner {

        private WebClient chatClient = WebClient.create("http://localhost:8080/chat");


        @Autowired
        private TriviaProperties triviaProperties;

        private Question currentQuestion = null;

        private AtomicLong skip = new AtomicLong();

        @Override
        public void run(String... args) throws Exception {
            askQuestion();
            chatClient.get().retrieve().bodyToFlux(ChatMessage.class).subscribe(message -> {
                if (message.getValue().equalsIgnoreCase(currentQuestion.getAnswer())) {
                    chatClient.post().syncBody(new ChatMessage("Congratz: \"" + message.from + "\" You're right!", "TriviaBot"))
                            .exchange().subscribe(response -> askQuestion());
                }

                if (message.getValue().equalsIgnoreCase("!t-info")) {
                    chatClient.post().syncBody(new ChatMessage("Commands are: "
                            + "<br> * \"!t-repeat\" to repeat the last question"
                            + "<br> * \"!t-skip\" to skip the current question ( 5 skips are required )"
                            + "<br> * \"!t-stats\" to print stats for best trivia players", "TriviaBot")).exchange().subscribe();
                }

                if (message.getValue().equalsIgnoreCase("!t-skip")) {
                    if (skip.incrementAndGet() >= 5) {
                        skip.set(0);
                        askQuestion();
                    }
                }

                if (message.getValue().equalsIgnoreCase("!t-repeat")) {
                    chatClient.post().syncBody(new ChatMessage(currentQuestion.getQuestion(), "TriviaBot")).exchange().subscribe();
                }

                if (message.getValue().equalsIgnoreCase("!t-stats")) {
                    chatClient.get().uri("/stats?user=TriviaBot&prefix=Congratz").retrieve().bodyToFlux(ChatMessage.class)
                            .collect(Collectors.groupingBy(m -> StringUtils.substringBetween(m.getValue(), "\"", "\""), Collectors.counting()))
                            .flatMapMany(m -> Flux.fromIterable(m.entrySet()))
                            .sort(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limitRequest(3)
                            .reduce(new ChatMessage("Top 3 Users for Trivia:", "TriviaBot"),
                                    (acc, entry) -> acc.addValue("<br> * User: " + entry.getKey() + " scored: " + entry.getValue()))
                            .subscribe(reduced -> chatClient.post().syncBody(reduced).exchange().subscribe());

                }

            });
        }

        private void askQuestion() {
            currentQuestion = triviaProperties.getRandomQuestion();
            chatClient.post().syncBody(new ChatMessage(currentQuestion.getQuestion(), "TriviaBot")).exchange().subscribe();
        }

    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ChatMessage {

        private String value;
        private String from;

        public ChatMessage addValue(String value) {
            this.value += value;
            return this;
        }

    }

    @Data
    @ConfigurationProperties("trivia")
    static class TriviaProperties {

        private List<Question> questions = new ArrayList<>();

        private final Random rd = new Random();

        public Question getRandomQuestion() {
            return questions.get(rd.nextInt(questions.size()));
        }

    }

    @Data
    static class Question {
        private String question;
        private String answer;
    }

}
