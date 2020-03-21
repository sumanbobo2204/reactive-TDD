package com.org.rsocket.server.greetingsstreamserver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.var;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.messaging.handler.invocation.reactive.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class GreetingsStreamServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreetingsStreamServerApplication.class, args);
    }
}

@Configuration
@EnableRSocketSecurity
class RsocketSecurityConfiguration {

    @Bean
    RSocketMessageHandler rSocketMessageHandler(RSocketStrategies strategies) {
        RSocketMessageHandler handler = new RSocketMessageHandler();
        handler.getArgumentResolverConfigurer().addCustomResolver(new AuthenticationPrincipalArgumentResolver());
        handler.setRSocketStrategies(strategies);
        return handler;
    }

    @Bean
    MapReactiveUserDetailsService authentication() {
        var bob = User.withDefaultPasswordEncoder().username("bob").password("bob").roles("USER", "ADMIN").build();
        var jlong = User.withDefaultPasswordEncoder().username("jlong").password("jlong").roles("USER").build();
        return new MapReactiveUserDetailsService(bob, jlong);
    }

    @Bean
    PayloadSocketAcceptorInterceptor authorization(RSocketSecurity security) {
        return security.authorizePayload(spec ->
                spec.route("greet").authenticated()
                        .anyExchange().permitAll()

        ).simpleAuthentication(Customizer.withDefaults())
                .build();
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class GreetingsResponse {
    private String message;
}

@Controller
class GreetingsController {

    @MessageMapping("greet")
    public Flux<GreetingsResponse> greetAPerson(@AuthenticationPrincipal Mono<UserDetails> user) {
        return user.map(ud -> ud.getUsername())
                .flatMapMany(GreetingsController::greet);
//        return ReactiveSecurityContextHolder.getContext()
//                .map(SecurityContext::getAuthentication)
//                .map(Principal::getName)
//                .flatMapMany(GreetingsController::greet);
    }

    private static Flux<GreetingsResponse> greet(final String name) {
        return Flux.fromStream(Stream.generate(
                () -> new GreetingsResponse("Hello !! " + name + "@ " + Instant.now().toString())))
                .delayElements(Duration.ofSeconds(1));
    }

}
