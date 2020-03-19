package com.bob.org.reactive.reservationservice;

import io.rsocket.RSocketFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Stream.generate;

@SpringBootApplication
public class ReservationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);

	}

}

/**
 * WebsocketConfig
 * Configuration of websocket for this App
 */
@Configuration
class WebsocketConfig {

	@Bean
	SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler webSocketHandler) {
		return new SimpleUrlHandlerMapping(){
			{
				setUrlMap(Collections.singletonMap("/ws/greetings", webSocketHandler));
				setOrder(10);
			}
		};
	}

	@Bean
	WebSocketHandlerAdapter webSocketHandlerAdapter() {
		return new WebSocketHandlerAdapter();
	}


	@Bean
	WebSocketHandler webSocketHandler(GreetingsProducer greetingsProducer) {
		return  new WebSocketHandler() {
			@Override
			public Mono<Void> handle(WebSocketSession webSocketSession) {
				Flux<WebSocketMessage> map = webSocketSession
						.receive()
						.map(WebSocketMessage::getPayloadAsText)
						.map(GreetingsRequest::new)
						.flatMap(greetingsProducer::greet)
						.map(GreetingsResponse::getResponString)
						.map(webSocketSession::textMessage);

				return webSocketSession.send(map);
			}
		};
	}
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingsRequest {
	private String name;
}


@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingsResponse {
	private String responString;
}


@Component
class GreetingsProducer {
	Flux<GreetingsResponse> greet(GreetingsRequest greetingsRequest) {
		/*try {
			Thread.sleep(40000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}*/
		return Flux.fromStream(generate(() ->
				new GreetingsResponse(" Hello " + greetingsRequest.getName() + Instant.now() + " !!")))
				.delayElements(Duration.ofSeconds(4));
	}
}

@Controller
@RequiredArgsConstructor
@Log4j2
class RsocketGreetingsController {

	private final GreetingsProducer greetingsProducer;

	@MessageMapping("greet")
	Flux<GreetingsResponse> greet(GreetingsRequest greetingsRequest) {
		log.info("Inside greet method");
		return this.greetingsProducer.greet(greetingsRequest);
	}
}

/**
 * HTTPConfiguration
 * Spring 5 , reactive functional style web framework.
 */
@Configuration
class  HTTPConfiguration {

	@Bean
	RouterFunction<ServerResponse> routes(ReservationRepository reservationRepository) {
		return RouterFunctions.route().GET("/reservations", serverRequest -> ServerResponse.ok().
				body(reservationRepository.findAll(), Reservation.class)).build();
	}
}

@Component
@RequiredArgsConstructor
@Log4j2
class sampleDataInitializer {

	final ReservationRepository reservationRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void goInit() {

	this.reservationRepository.deleteAll()
			.thenMany(
					Flux.just("Josh", "Madhura", "Rob", "Eric", "Phill")
					.map(r -> new Reservation(null, r))
					.flatMap(this.reservationRepository::save)

			)
			.thenMany(this.reservationRepository.findAll())
			.subscribe(log::info);

	}
}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, String> {

}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
class Reservation {
@Id
private String Id;
private String name;
}
