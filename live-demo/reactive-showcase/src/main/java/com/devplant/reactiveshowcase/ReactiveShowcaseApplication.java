package com.devplant.reactiveshowcase;

import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import reactor.core.publisher.Flux;

@SpringBootApplication
public class ReactiveShowcaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveShowcaseApplication.class, args);
	}
}

//project lombok to generate getters/setter for us
@Data
class DataObject {
	private long firstThreadId;
	private long secondThreadId;
}

@RestController
class ShowcaseCtrl {

	@GetMapping(value = "/threads",
			produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> test() {
		// This is just a blueprint, nothing happens here
		Flux<DataObject> dataFlux = Flux.generate(s ->
				s.next(new DataObject()));
		// This is also just a blueprint, nothing happens
		// until you subscribe
		Flux<String> mapped = dataFlux
				// delay elements for UI's sake
				.delayElements(Duration.ofMillis(500))
				.map(dataObject -> {
					dataObject.setFirstThreadId(Thread.currentThread().getId());
					return dataObject;
				})
				// delay elements to allow a thread-switch
				.delayElements(Duration.ofMillis(250))
				.map(dataObject -> {
					dataObject.setSecondThreadId(Thread.currentThread().getId());
					return dataObject;
				}).map(dataObject -> "Executed by: "
						+ dataObject.getFirstThreadId()
						+ " / " + dataObject.getSecondThreadId());
		// Flux is returned, browser subscribes to server-sent-events
		return mapped;
	}

}

