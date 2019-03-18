package com.org.bob.reactive.producerapp;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
@ComponentScan(basePackages = {"com.org.bob.reactive.producerapp.*"})
@RestController
@Log4j2
public class ProducerAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProducerAppApplication.class, args);
	}



	// For checking reactor context :::

	//scheduler creation for creating some extra threads for the purpose ::
	private static Scheduler SCHEDULER = Schedulers.fromExecutor(Executors.newFixedThreadPool(10));


	private static <T> Flux<T> prepareData(Flux<T> in) {
		return in.doOnNext(log::info).subscribeOn(SCHEDULER);
	}


	public Flux<String> dataEmit() {
		Flux<String> names = prepareData(Flux.just("Bob", "Alice", "Josh", "Eric", "Hecler", "Martin"));
		Flux<Integer> age = prepareData(Flux.just(12, 34, 45, 67, 34, 70));

		Flux<String> mergerdData = prepareData(Flux.zip(names, age).map(tuple -> tuple.getT1() + "::" + tuple.getT2()))
				.doOnEach(stringSignal -> {
					if(!stringSignal.isOnNext()){
						return;
					}
					Object userID = stringSignal.getContext().get("userId");
					log.info("userID for this reactive pipeline stage for data " + stringSignal.get()+ " is " +userID);
				})
				.subscriberContext(Context.of("userId", UUID.randomUUID().toString()))

				;
		return mergerdData;
	}


	@GetMapping("/data")
	Flux<String> getData() {
		return dataEmit();
	}



}



