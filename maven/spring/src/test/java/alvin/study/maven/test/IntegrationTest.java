package alvin.study.maven.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import alvin.study.maven.Application;
import alvin.study.maven.test.conf.TestContextInitializer;

@SpringBootTest(classes = { Application.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { TestContextInitializer.class })
@ActiveProfiles("test")
public abstract class IntegrationTest extends UnitTest {}
