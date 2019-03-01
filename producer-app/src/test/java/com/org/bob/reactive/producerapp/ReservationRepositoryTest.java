package com.org.bob.reactive.producerapp;

import com.org.bob.reactive.producerapp.pojo.Reservation;
import com.org.bob.reactive.producerapp.repository.ReservationRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


@RunWith(SpringRunner.class)
@DataMongoTest
public class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    public void findAllByReservationName() throws Exception {

        Flux<Reservation> reservationFlux =
                this.reservationRepository.deleteAll()
                .thenMany(Flux.just("A", "B", "C", "D", "C")
                        .flatMap(name -> this.reservationRepository.save(new Reservation(null, name)))
                )
                .thenMany(this.reservationRepository.findByReservationName("C"));


        StepVerifier
                .create(reservationFlux)
                .expectNextCount(2)
                .verifyComplete();

    }

}
