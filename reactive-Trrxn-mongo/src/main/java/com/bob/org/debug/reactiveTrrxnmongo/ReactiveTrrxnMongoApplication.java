package com.bob.org.debug.reactiveTrrxnmongo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/**
 *
 */
@SpringBootApplication
public class ReactiveTrrxnMongoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReactiveTrrxnMongoApplication.class, args);

	}

	@Bean
	TransactionalOperator transactionalOperator(ReactiveTransactionManager reactiveTransactionManager) {
		return TransactionalOperator.create(reactiveTransactionManager);
	}

	@Bean
	ReactiveTransactionManager transactionManager(ReactiveMongoDatabaseFactory databaseFactory) {
		return  new ReactiveMongoTransactionManager(databaseFactory);
	}

}


@Service
@RequiredArgsConstructor
class CustomerService {

	final TransactionalOperator transactionalOperator;
	final CustomerRepository customerRepository;

	/**
	 * @param emails
	 * @return
	 */
	public Flux<Customer> saveCustomers(String...emails) {

		Flux<Customer> customerFlux = Flux.just(emails)
				.map(email -> new Customer(null, email))
				.flatMap(customerRepository::save)
				.doOnNext(record -> Assert.isTrue(record.getEmail().contains("@"), "The Email must contain @"));

		//return customerFlux;
		return this.transactionalOperator.execute(s -> customerFlux);
	}

}


interface  CustomerRepository extends ReactiveCrudRepository<Customer, String> {

}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
class Customer{
	@Id
	private  String id;
	private String email;
}