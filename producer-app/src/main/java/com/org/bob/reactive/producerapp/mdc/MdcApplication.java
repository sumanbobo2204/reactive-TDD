package com.org.bob.reactive.producerapp.mdc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SpringBootApplication
@Log4j2
@RestController
public class MdcApplication {

    private final RestaurantService restaurantService;

    public MdcApplication(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/{uid}/restaurants/{maxPrice}")
    public Flux<Restaurant> getResByMaxPrice(@PathVariable String uid, @PathVariable double maxPrice){
       return adaptResultsForContextualLogging(this.restaurantService.getByMaxPrice(maxPrice), uid, maxPrice);
    }

    private Flux<Restaurant> adaptResultsForContextualLogging(Flux<Restaurant> byMaxPrice, String uid, double maxPrice) {
        return Mono.just(String.format("finding restaurants having price lower than $%.2f for %s", maxPrice, uid))
                .doOnEach(logOnNext(log::info))
                .thenMany(byMaxPrice)
                .doOnEach(logOnNext( r -> log.info("found restaurant {} for ${}", r.getResName(), r.getPricePerPerson())))
                .subscriberContext(Context.of("apiId", uid));
    }


    private static <T> Consumer<Signal<T>> logOnNext(Consumer<T> logStatement) {
        return tSignal -> {
            if( ! tSignal.isOnNext()) {
                return;
            }
            Optional<String> apiId = tSignal.getContext().getOrEmpty("apiId");

            Consumer<String> ifPresent = id -> {
                try(MDC.MDCCloseable closable  = MDC.putCloseable("apiId", id)) {
                    Runnable r = () -> logStatement.accept(tSignal.get());
                    r.run();
                }
            };
            apiId.ifPresent(ifPresent);
        };
    }

}

@Data
@NoArgsConstructor
@AllArgsConstructor
class Restaurant {
    private double pricePerPerson;
    private String resName;

}

@Service
class RestaurantService{

    private final Collection<Restaurant> restaurantCollection = new ConcurrentSkipListSet<> ((o1, o2) -> {
            Double one = o1.getPricePerPerson();
            Double two = o2.getPricePerPerson();
            return one.compareTo(two);
    });

    // Random restaurant instance creation and adding them to the concurrent collection:::
    RestaurantService() {
        IntStream.range(0, 1000)
                .mapToObj(Integer::toString)
                .map(r -> "Restaurant # " +r)
                .map(res -> new Restaurant(new Random().nextDouble() * 100, res))
                .forEach(this.restaurantCollection::add);
    }


    Flux<Restaurant> getByMaxPrice(double maxPrice) {

        return Flux.fromStream(this.restaurantCollection
                .parallelStream()
                .filter(i -> i.getPricePerPerson() <= maxPrice))
        ;
    }
}
