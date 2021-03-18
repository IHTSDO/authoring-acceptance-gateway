package org.snomed.aag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.snomed.aag.data.repositories.TestEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(classes = TestConfig.class)
public class AbstractTest {

	@Autowired
	private TestEntityRepository testEntityRepository;

	@AfterEach
	void defaultTearDown() {
		testEntityRepository.deleteAll();
	}

}
