package com.org.bob.reactive.producerapp.repository;

import com.org.bob.reactive.producerapp.pojo.Reservation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface ReservationRepository extends ReactiveMongoRepository<Reservation, String> {

    Flux<Reservation> findByReservationName(String rn);
}
