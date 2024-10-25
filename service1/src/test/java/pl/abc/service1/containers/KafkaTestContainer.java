package pl.abc.service1.containers;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@ActiveProfiles("test")
@Testcontainers
public interface KafkaTestContainer {

    String DOCKER_IMAGE_NAME = "apache/kafka";

    @Container
    KafkaContainer container = new KafkaContainer(DOCKER_IMAGE_NAME);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.producer.bootstrap-servers", container::getBootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", container::getBootstrapServers);
    }
}
