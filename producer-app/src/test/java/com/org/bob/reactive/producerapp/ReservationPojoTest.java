package com.org.bob.reactive.producerapp;

import com.org.bob.reactive.producerapp.pojo.Reservation;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReservationPojoTest {

    @Test
    public void testReservationPojo() {
        Reservation r = new Reservation(null, "bob");
        Assert.assertNull(r.getId());
        Assert.assertThat(r.getReservationName(), Matchers.equalTo("bob"));
        Assertions.assertThat(r.getReservationName()).isEqualToIgnoringCase("bob");

    }

}
