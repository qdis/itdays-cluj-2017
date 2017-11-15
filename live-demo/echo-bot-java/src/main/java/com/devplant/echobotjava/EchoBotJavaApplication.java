package com.devplant.echobotjava;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SpringBootApplication
public class EchoBotJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoBotJavaApplication.class, args);
    }

    @Bean
    WebClient webClient(){
        return WebClient.create("http://localhost:8080/chat");
    }

    @Bean
    CommandLineRunner echo(WebClient webClient){
        return args->{
            webClient.get().retrieve().bodyToFlux(Message.class).subscribe(
                    message -> {
                        if(!message.getFrom().equalsIgnoreCase("Echo")){
                            webClient.post().syncBody(new Message("Echo",
                                    "Echoing from bot: "+message.getValue()))
                                    .exchange().subscribe();
                        }
                    }
            );
        };
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Message {

    private String from;
    private String value;
}