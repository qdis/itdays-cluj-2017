package com.devplant.chatshell;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@SpringBootApplication
public class ChatShellApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatShellApplication.class, args);
    }

    @Bean
    WebClient webClient() {
        return WebClient.create("http://localhost:8080/chat");
    }

    @ShellComponent
    class LoginShell {

        @Autowired
        private WebClient webClient;

        @PostConstruct
        protected void postConstruct() {
            webClient.get().retrieve().bodyToFlux(Message.class).subscribe(m -> System.out.println(m.getFrom() + ": " + m.getValue()));
        }

        @ShellMethod("Send messages yo our chat")
        void send(@ShellOption String message) {
            webClient.post().syncBody(new Message("Chat Shell", message))
                    .exchange().subscribe();
        }
    }
}


@Data
@NoArgsConstructor
@AllArgsConstructor
class Message {
    private String from;
    private String value;
}