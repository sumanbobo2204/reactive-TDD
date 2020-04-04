package com.org.bob.reactivecircuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ReactiveCircuitBreakerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveCircuitBreakerApplication.class, args);
	}

	@Bean
	WebClient webClient(WebClient.Builder builder) {
		var client = builder
				.baseUrl("http://localhost:8080")
				.build();
		return client;
	}

}

@Configuration
@RequiredArgsConstructor
@Log4j2
class ReactiveReservationClient {

	private final WebClient webClient;

	private final ReactiveCircuitBreakerFactory circuitBreakerFactory;

	@EventListener(ApplicationReadyEvent.class)
	public void ready() {

//		Flux<String> host1 = null;//todo
//		Flux<String> host2 = null;//todo
//		Flux<String> host3 = null;//todo
//
//		Flux<String> first = Flux.first(host1, host2, host3); //todo

		var reactiveCircuitBreaker = circuitBreakerFactory.create("rcb");

		reactiveCircuitBreaker.run(
				this.webClient
						.get()
						.uri("/reservations")
						.retrieve()
						.bodyToFlux(Reservation.class)
						.map(x -> x.getName()),
				throwable -> Flux.just("EEKK !!"))

				.subscribe(data -> log.info("names : " +data));
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {

	private String Id;
	private String name;
}
