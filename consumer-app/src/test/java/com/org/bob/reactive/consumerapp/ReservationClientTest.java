package com.org.bob.reactive.consumerapp;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

//@AutoConfigureWireMock(port = 8080)
@AutoConfigureStubRunner(ids = "com.org.bob.reactive:producer-app:+:8080",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReservationClientTest {

    @Autowired
    private ReservationClient reservationClient;

    @Test
    public void getAllReservations() throws Exception {

        // WireMock stub generation to fake the rest api ::
        /*WireMock
                .stubFor(WireMock.any(WireMock.urlMatching("/reservations"))
                .willReturn(WireMock.aResponse()
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
                .withStatus(HttpStatus.OK.value())
                .withBody("[{\"id\":\"1\",\"name\":\"jane\"},{\"id\":\"2\",\"name\":\"bob\"}]")));*/


        Flux<Reservation> allReservations = this.reservationClient.getAllReservations();

        Predicate<Reservation> pr = r -> (r.getReservationName().equals("A") || r.getReservationName().equals("B"))
                && StringUtils.hasText(r.getId());

        StepVerifier
                .create(allReservations)
                .expectNextMatches(pr)
                .expectNextMatches(pr)
                .verifyComplete();
    }

}
