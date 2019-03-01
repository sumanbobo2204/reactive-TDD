package com.org.bob.reactive.producerapp.controller;

import com.org.bob.reactive.producerapp.pojo.Reservation;
import com.org.bob.reactive.producerapp.repository.ReservationRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class ReservationRestConfig {

    @Bean
    RouterFunction<ServerResponse> routes(ReservationRepository reservationRepository) {

    return RouterFunctions.route(GET("/reservations"), serverRequest ->
        ServerResponse.ok().contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(reservationRepository.findAll(), Reservation.class));
    }
}
