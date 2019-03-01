package com.org.bob.reactive.producerapp;

import com.org.bob.reactive.producerapp.pojo.Reservation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.function.Predicate;

@DataMongoTest
@RunWith(SpringRunner.class)
public class ReservationDocumentTest {

    @Autowired
    private ReactiveMongoTemplate reactiveMongoTemplate;

    @Test
    public void testReservationDocumentPersist() throws  Exception {

        Flux<Reservation> savedRecord = Flux.just(new Reservation(null, "wallace"),
                                                    new Reservation(null, "NJR"))
                .flatMap(r -> this.reactiveMongoTemplate.save(r));

        Flux<Reservation> interaction = this.reactiveMongoTemplate
                .dropCollection(Reservation.class)
                .thenMany(savedRecord)
                .thenMany(this.reactiveMongoTemplate.findAll(Reservation.class));

        Predicate<Reservation> pr = r -> StringUtils.hasText(r.getId()) && (
                                        r.getReservationName().equals("wallace")
                                        || r.getReservationName().equals("NJR"));

        //StepVerifier for mongo persistance check..
        StepVerifier
                .create(interaction)
                .expectNextMatches(pr)
                .expectNextMatches(pr)
                .verifyComplete();


        // Strpverifier with virtual time implementation ::

        StepVerifier.withVirtualTime(() -> {return Flux.just("A","B","C").
                delayElements(Duration.ofSeconds(2));})
                .thenAwait(Duration.ofDays(7))
                .expectNextCount(3)
                .verifyComplete();

    }
}
