package com.org.bob.reactive.producerapp;

import com.org.bob.reactive.producerapp.controller.ReservationRestConfig;
import com.org.bob.reactive.producerapp.pojo.Reservation;
import com.org.bob.reactive.producerapp.repository.ReservationRepository;
import io.restassured.RestAssured;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

@SpringBootTest(properties= { "server.port = 0" },
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@RunWith(SpringRunner.class)
@Import(ReservationRestConfig.class)
public class BaseClass {

    @LocalServerPort
    private int port;

    @MockBean
    private ReservationRepository reservationRepository;

    @Before
    public void setUp() {
        Mockito.when(this.reservationRepository.findAll()).thenReturn(
                Flux.just(new Reservation("1","A"), new Reservation("2","B"))
        );

        RestAssured.baseURI = "http://localhost:" +this.port;
    }

}
