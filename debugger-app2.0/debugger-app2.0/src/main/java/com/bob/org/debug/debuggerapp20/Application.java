package com.bob.org.debug.debuggerapp20;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.tools.agent.ReactorDebugAgent;

import java.time.Duration;

@SpringBootApplication
@Log4j2
public class Application {

	public static void main(String[] args) {

		// Injects debug on reactor operators ..
		//Hooks.onOperatorDebug();

		//Debug Agent from reactor tools ..
		//ReactorDebugAgent.init();
		//ReactorDebugAgent.processExistingClasses();

		//Java agent to detect blocking calls from non-blocking threads.
		BlockHound.install();


		SpringApplication.run(Application.class, args);
	}

	static class IllegalLetterException extends Exception {
		IllegalLetterException() {
			super("Letter cant be F");
		}
	}

	void infoLogging(String letter) {
		log.info("THREAD :: " +Thread.currentThread().getName());
		log.info("Current letter value is :: " + letter);
	}

	void errorLoggin(Throwable t) {
		log.error("THREAD :: " +Thread.currentThread().getName());
		log.error("OH NOES !!");
		log.error(t);
	}

	@SneakyThrows
	void block() {
		Thread.sleep(2000);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void goDebugg() {

		// This is elastic scheduler so it can produce more threads on the go
		Scheduler schedulersElastic = Schedulers.elastic();

		// This can create atmost 3 threads so blocking call we can detect via blockHound
		Scheduler schedulerCore = Schedulers.newParallel("p3", 3);

		Flux<String> letters = Flux.just("A", "X", "W", "F", "K", "O", "T", "F")
				.checkpoint("Capital letters" , true)
				/*.flatMap(letter -> {
					if (letter.equalsIgnoreCase("F"))
						return Mono.error(new IllegalLetterException());
					else return Mono.just(letter);
				})*/
				.flatMap(Mono::just)
				.doOnNext(x -> block())
				.checkpoint(" this message is shown by error checkpoint" , true)
				.subscribeOn(schedulerCore, true)
				.log()
				.delayElements(Duration.ofSeconds(2))
				.doOnError(this::errorLoggin);

		processLetterFlux(letters);

	}

	private void processLetterFlux(Flux<String> letters) {
		letters.subscribe(this::infoLogging);
	}



}
