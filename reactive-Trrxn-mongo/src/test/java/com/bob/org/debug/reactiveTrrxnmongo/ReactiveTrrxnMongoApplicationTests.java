package com.bob.org.debug.reactiveTrrxnmongo;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

@SpringBootTest
@RunWith(SpringRunner.class)
class ReactiveTrrxnMongoApplicationTests {

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	CustomerService customerService;


	@Test
	public void testCustomerTransactions() throws Exception {

		StepVerifier.create(this.customerRepository.deleteAll()).verifyComplete();

		StepVerifier
				.create(this.customerRepository.findAll())
				.expectNextCount(0)
				.verifyComplete();

		StepVerifier.create(this.customerService.saveCustomers("s@ff", "u@ff", "ww@rr", "ff&@rrr"))
					.expectNextCount(4)
				.verifyComplete();

	}

}
