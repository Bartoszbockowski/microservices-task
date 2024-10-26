package abc.service2.configuration;

import abc.service2.model.event.BookEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.postgresql.util.PSQLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.sql.SQLException;

@Slf4j
@Configuration
public class KafkaConfiguration {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, BookEvent> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (r, e) -> {
                    String topic = r.topic() + ".failures";
                    log.info("Error occurred during {} event processing:", topic, e);
                    log.info("Adding dead-letter event={} to the topic={}", r.value(), topic);
                    return new TopicPartition(topic, r.partition());
                });
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(3000L, 2L));
        errorHandler.addNotRetryableExceptions(SQLException.class);
        errorHandler.addNotRetryableExceptions(PSQLException.class);
        return errorHandler;
    }
}
