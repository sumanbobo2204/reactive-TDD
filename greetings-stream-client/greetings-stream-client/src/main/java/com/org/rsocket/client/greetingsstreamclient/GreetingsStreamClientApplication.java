package com.org.rsocket.client.greetingsstreamclient;

import io.rsocket.metadata.WellKnownMimeType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.SimpleAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootApplication
@Log4j2
public class GreetingsStreamClientApplication {

	@SneakyThrows
	public static void main(String[] args) {
		SpringApplication.run(GreetingsStreamClientApplication.class, args);
		System.in.read();
	}

	private final MimeType mimeType = MimeTypeUtils.
			parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.getString());

	private final UsernamePasswordMetadata credentials =
			new UsernamePasswordMetadata("bob", "bob");

	@Bean
	RSocketStrategiesCustomizer rSocketStrategiesCustomizer() {
		return strategies -> strategies.encoder(new SimpleAuthenticationEncoder());
	}

	@Bean
	RSocketRequester requester(RSocketRequester.Builder builder) {
		return builder.connectTcp("localhost", 8888)
				.block();
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> applicationListener(RSocketRequester greetingsClient) {
		return applicationReadyEvent -> {
			greetingsClient
					.route("greet")
					.metadata(this.credentials, this.mimeType)
					.data(Mono.empty())
					.retrieveFlux(GreetingsResponse.class)
					.subscribe(data -> log.info("secured response -> " + data.toString()));
		};
	}
}


@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingsResponse {
	private String message;
}
