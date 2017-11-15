package com.devplant.chatservicejava;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.ServerResponse;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Simple Application created with spring initializer
 * https://start.spring.io/
 */
@SpringBootApplication
public class ChatServiceJavaApplication {

    @Bean
    CommandLineRunner cmd(MongoOperations mongoOperations){
        return args -> {
            mongoOperations.createCollection("messages",
                    new CollectionOptions(100000l,1000l,
                            true));
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ChatServiceJavaApplication.class, args);
    }
}

@Controller
class IndexCtrl{

    @GetMapping
    public String indx(){
        return "index";
    }
}

@RestController
class ChatCtrl{

    @Autowired
    private MessageRepo messageRepo;

    @GetMapping(value = "/chat",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Message> messages(){
       return  messageRepo.findAllByDateAfter(new Date());
    }

    @PostMapping
    public Mono<ServerResponse> postMessage(@RequestBody Message message){
        message.setDate(new Date());
        return messageRepo.save(message).then(ServerResponse.ok().build());
    }

}


interface MessageRepo extends ReactiveMongoRepository<Message,String>{

    @Tailable
    Flux<Message> findAllByDateAfter(Date now);
}


@Data
@Document(collection = "messages")
class Message {

    private String from;
    private String value;
    private Date date;
}