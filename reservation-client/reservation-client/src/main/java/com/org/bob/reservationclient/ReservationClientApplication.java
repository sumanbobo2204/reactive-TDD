package com.org.bob.reservationclient;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreaker;
import org.springframework.cloud.circuitbreaker.commons.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Mono;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootApplication
@EnableConfigurationProperties(UriConfig.class)
public class ReservationClientApplication {

	@Bean
	ApplicationRunner client() {
		return  args -> {
			WebClient client = WebClient.builder().
					filter(ExchangeFilterFunctions.basicAuthentication("user","pw")).build();

			Flux.fromStream(IntStream.range(0 , 100).boxed())
					.flatMap(r -> client.get().uri("http://localhost:9090/rl").exchange())
					.flatMap(r -> r.toEntity(String.class))
					.map(r -> String.format("status: %s; body: %s", r.getStatusCode(), r.getBody()))
					.subscribe(System.out::println);

		};

	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationClientApplication.class, args);
	}

	/**
	 * circuitBreakerFactory.
	 * Reactive spring cloud circuit breaker config
	 * @return {@link ReactiveCircuitBreakerFactory}
	 */
	@Bean
	ReactiveCircuitBreakerFactory circuitBreakerFactory() {
		ReactiveResilience4JCircuitBreakerFactory factory = new ReactiveResilience4JCircuitBreakerFactory();
		factory
				.configureDefault(s -> new Resilience4JConfigBuilder(s)
						.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(5)).build())
						.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
						.build());
		return factory;
	}


	/**
	 * Redis Rate Limitter Config ::
	 * @return
	 */
	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(5, 7);
	}

	/**
	 * authentication.
	 * spring security authentication config.
	 * @return {@link MapReactiveUserDetailsService}
	 */
	@Bean
	MapReactiveUserDetailsService authentication() {
		return new MapReactiveUserDetailsService(
				User.withDefaultPasswordEncoder()
						.username("user")
						.password("pw")
						.roles("USER")
						.build());
	}

	/**
	 * authorization.
	 * spring security authorization config.
	 * @param httpSecurity
	 * @return {@link SecurityWebFilterChain}
	 */
	@Bean
	SecurityWebFilterChain authorization(ServerHttpSecurity httpSecurity) {

		ReactiveAuthorizationManager<AuthorizationContext> reactiveAuthorizationManager =
				new ReactiveAuthorizationManager<AuthorizationContext>() {
			@Override
			public Mono<AuthorizationDecision> check(Mono<Authentication> authentication,
													 AuthorizationContext object) {
				Mono<Boolean> booleanMono = authentication.map(p -> p.getAuthorities().stream().
						anyMatch(a -> ((GrantedAuthority) a).getAuthority().toLowerCase().contains("USER")));
				return booleanMono.map(AuthorizationDecision::new);
			}
		};
		//@formatter:off
		return httpSecurity
				.csrf().disable()
				.authorizeExchange()
					.pathMatchers("/rl").authenticated()
					.pathMatchers("/any").access(reactiveAuthorizationManager)
				.anyExchange().permitAll()
				.and()
				.httpBasic()
				.and()
				.build();
		//@formatter:on
	}

	/**
	 * gateWayRoutes.
	 * proxying the Edege service uris with Spring cloud gateway ::
	 * @param builder
	 * @param uriConfig
	 * @return {@link RouteLocator}
	 */

	@Bean
	RouteLocator gateWayRoutes(RouteLocatorBuilder builder, UriConfig uriConfig ) {
		return builder
				.routes()
				.route(rspec -> rspec.host("*").and().path("/proxy/{name}")
						.filters( fSpec -> fSpec.setPath("/greetings/{name}")
								.addResponseHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*"))
						.uri(uriConfig.getBaseUri()))

				.route(r -> r.host("*").and().path("/rl")
							.filters( f -> f.setPath("/reservations/names")
									.addResponseHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
									.requestRateLimiter(config -> redisRateLimiter()))
						.uri(uriConfig.getBaseUri()))

				.route(rs -> rs.host("*").and().path("/proxydelay")
							.filters(f -> f.setPath("/delay").hystrix(config -> config.setName("cb")
																			.setFallbackUri("forward:/fallback")))
						.uri(uriConfig.getBaseUri()))

				.build();
	}

	/**
	 * rSocketRequester.
	 * @param builder
	 * @return {@link RSocketRequester}
	 */
	@Bean
	RSocketRequester rSocketRequester(RSocketRequester.Builder builder) {
		return builder.connect(TcpClientTransport.create(7000)).block();
	}

	/*@Bean
	public RSocket rSocket() {
		return RSocketFactory
				.connect()
				.dataMimeType(MimeTypeUtils.APPLICATION_JSON_VALUE)
				.frameDecoder(PayloadDecoder.ZERO_COPY)
				.transport(TcpClientTransport.create(9999))
				.start()
				.block();
	}

	@Bean
	RSocketRequester rSocketRequester(RSocketStrategies rSocketStrategies) {
		//return RSocketRequester.wrap(rSocket(), MimeTypeUtils.APPLICATION_JSON, rSocketStrategies);
		return RSocketRequester.wrap(rSocket(), MimeTypeUtils.APPLICATION_JSON
				, MimeTypeUtils.APPLICATION_JSON , rSocketStrategies);
	}*/

	@Bean
	WebClient webClient(WebClient.Builder builder, LoadBalancerExchangeFilterFunction filterFunction)
	{
		return 	builder
				.filter(filterFunction)
				.build();
	}
/*
	@Bean
	LoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(LoadBalancerClient client) {
		return new LoadBalancerExchangeFilterFunction(client);
	}
*/

	@Bean
	RouterFunction<?> routes() {
		return route()
				.GET("/hi" , request -> {
					Publisher<String> publisher = Mono.just("Hi");

					Publisher<String> hystrixPublisher = HystrixCommands
							.from(publisher)
							.fallback(Flux.just("EEKKK !!! "))
							.commandName("fallback-cmd").eager().build();

					return ServerResponse.ok().body(hystrixPublisher , String.class);
				})

				.build();
	}

	/**
	 * serverResponseRouterFunction.
	 * @param app
	 * @param gc
	 * @return
	 */
	@Bean
	RouterFunction<?> reservationRoutes(ReservationClientSideApp app,
																GreetingsClient gc ) {
		return route()
				.GET("/greetings/{name}" , serverRequest -> {
					String name = serverRequest.pathVariable("name");
					Flux<GreetingsResponse> greet = gc.greet(new GreetingsRequest(name))
							.retryBackoff(5 , Duration.ofSeconds(2));

					return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
							.body(greet, GreetingsResponse.class);
				})

				.GET("/reservations/names" , serverRequest -> {
					Flux<String> map = app.getAllReservations()
							.map(Reservation::getName)
							.onErrorResume(throwable -> Flux.just("EEEKK!!"))
							.retryBackoff(5 , Duration.ofSeconds(1));
					return ServerResponse.ok().body(map, String.class);

				})
				.GET("/delay", request -> ServerResponse.ok().body(Flux.just("Hello Delay!!")
						.delayElements(Duration.ofSeconds(10)), String.class))

				.GET("/fallback", request -> ServerResponse.ok().body(Mono.just("Hystrix fallback"), String.class))

				.build();
	}
}

/**
 * UriConfig.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties
class UriConfig {
	private  String baseUri = "http://localhost:9090";
}


@Data
@NoArgsConstructor
@AllArgsConstructor
class Reservation {
	private String Id;
	private String name;
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
@RequiredArgsConstructor
class GreetingsClient {

	private final RSocketRequester rSocketRequester;

	Flux<GreetingsResponse> greet(GreetingsRequest greetingsRequest) {
		return this.rSocketRequester
				.route("greet")
				.data(greetingsRequest)
				.retrieveFlux(GreetingsResponse.class);
	}
}

@Component
@RequiredArgsConstructor
class ReservationClientSideApp {

	private final WebClient webClient;

	Flux<Reservation> getAllReservations() {
		return this.webClient
				.get()
				.uri("http://localhost:8080/reservations")
				.retrieve()
				.bodyToFlux(Reservation.class);
	}

}

// Mocked version of a failing service for testing the
// Spring cloud circuit breaker resilience4j implementation .
@Log4j2
@Service
class FailingService {

	Mono<String> greet(Optional<String> name) {
		long seconds = (long) (Math.random() * 10);

		return name
				.map(str -> {
					String msg = "Hello " + str + "! (in " + seconds + ")";
					log.info(msg);
					return Mono.just(msg);
				})
				.orElse(Mono.error(new NullPointerException()))
				.delayElement(Duration.ofSeconds(seconds));
	}
}


// Rest controller for spring cloud circuit breaker testing ::
@RestController
class FailingRestController {

	private final FailingService failingService;
	private final ReactiveCircuitBreaker circuitBreaker;

	FailingRestController(FailingService fs,
						  ReactiveCircuitBreakerFactory cbf) {
		this.failingService = fs;
		this.circuitBreaker = cbf.create("greet");
	}

	@GetMapping("/greet")
	Publisher<String> greet(@RequestParam Optional<String> name) {
		final Mono<String> results = this.failingService.greet(name);// cold
		return this.circuitBreaker.run(results, throwable -> Mono.just("hello world!"));
	}
}