package com.devplant.chatservice;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

import java.util.Date;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;


@Slf4j
@SpringBootApplication
public class ChatServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

//    @Bean
//    CommandLineRunner initializer(MongoOperations mongoOperations) {
//        return args ->
//                mongoOperations.createCollection("messages",
//                        new CollectionOptions(1000000L, 1000L, true));
//
//    }

    @Bean
    SubscribableChannel messageChannel() {
        return MessageChannels.publishSubscribe().get();
    }

    @Bean
    RouterFunction<?> routes(@Qualifier("messageChannel") SubscribableChannel messagesChannel, ChatRepo chatRepo) {
        Whitelist whitelist = Whitelist.basic();
        return RouterFunctions

                .route(GET("/chat"),
                        serverRequest -> ServerResponse.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(Flux.create(sink -> {
                                    MessageHandler handler = msg -> sink.next(ChatMessage.class.cast(msg.getPayload()));
                                    sink.onCancel(() -> messagesChannel.unsubscribe(handler));
                                    messagesChannel.subscribe(handler);
                                }), ChatMessage.class))
                .andRoute(GET("/chat/stats"),
                        serverRequest -> ServerResponse.ok().body(
                                chatRepo.findByFromAndValueStartingWith(serverRequest.queryParam("user").orElse(null),
                                        serverRequest.queryParam("prefix").orElse(null)), ChatMessage.class))
                .andRoute(POST("/chat"),
                        serverRequest -> serverRequest.bodyToMono(ChatMessage.class).flatMap(message -> {
                            message.setDate(new Date());
                            message.setValue(Jsoup.clean(message.getValue(), whitelist).trim());
                            message.setFrom(Jsoup.clean(message.getFrom(), whitelist).replaceAll("\"", "").trim());
                            if (!message.getFrom().isEmpty() && !message.getValue().isEmpty()) {
                                return chatRepo.save(message).flatMap(saved -> {
                                    messagesChannel.send(new GenericMessage<>(message));
                                    return ServerResponse.ok().build();
                                });
                            } else {
                                return ServerResponse.badRequest().build();
                            }
                        }));
    }

    @Controller
    class Home {

        @GetMapping("/")
        public String home() {
            return "index";
        }
    }

//
//    @RestController
//    @RequestMapping("/chat")
//    class ChatController {
//
//        @Autowired
//        private ChatRepo chatRepo;
//
//        private Whitelist whitelist = Whitelist.basic();
//
//        @Autowired
//        @Qualifier("messageChannel")
//        private SubscribableChannel messagesChannel;
//
//        @PostMapping
//        public Mono<ServerResponse> postMessage(@RequestBody ChatMessage message) {
////            message.setDate(new Date());
////            message.setValue(Jsoup.clean(message.getValue(), whitelist));
////            message.setFrom(Jsoup.clean(message.getFrom(), whitelist).replaceAll("\"", ""));
////            if (!message.getFrom().isEmpty() && !message.getValue().isEmpty()) {
////                return chatRepo.save(message).then(ServerResponse.ok().build());
////            } else {
////                return ServerResponse.badRequest().build();
////            }
//            message.setDate(new Date());
//            message.setValue(Jsoup.clean(message.getValue(), whitelist).trim());
//            message.setFrom(Jsoup.clean(message.getFrom(), whitelist).replaceAll("\"", "").trim());
//            if (!message.getFrom().isEmpty() && !message.getValue().isEmpty()) {
//                return chatRepo.save(message).flatMap(saved -> {
//                    messagesChannel.send(new GenericMessage<>(message));
//                    return ServerResponse.ok().build();
//                });
//            } else {
//                return ServerResponse.badRequest().build();
//            }
//        }
//
//        @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//        public Flux<ChatMessage> messageStream() {
//            // return chatRepo.findByDateAfter(new Date());
//            return Flux.create(sink -> {
//                MessageHandler handler = msg -> sink.next(ChatMessage.class.cast(msg.getPayload()));
//                sink.onCancel(() -> messagesChannel.unsubscribe(handler));
//                messagesChannel.subscribe(handler);
//            });
//        }
//
//        @GetMapping("/stats")
//        public Flux<ChatMessage> getByUsernameAndPrefix(@RequestParam("user") String user, @RequestParam("prefix") String prefix) {
//            return chatRepo.findByFromAndValueStartingWith(user, prefix);
//        }
//    }


}

@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
class ChatMessage {

    private String value;
    private String from;
    private Date date;

}

interface ChatRepo extends ReactiveMongoRepository<ChatMessage, String> {

//    @Tailable
//    Flux<ChatMessage> findByDateAfter(Date when);

    Flux<ChatMessage> findByFromAndValueStartingWith(String from, String value);

}

