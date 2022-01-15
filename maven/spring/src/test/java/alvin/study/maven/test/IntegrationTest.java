package alvin.study.maven.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import alvin.study.maven.Application;
import alvin.study.maven.test.conf.TestContextInitializer;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { Application.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { TestContextInitializer.class })
@ActiveProfiles("test")
public abstract class IntegrationTest extends UnitTest {
}
