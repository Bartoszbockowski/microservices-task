package abc.service2.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.sql.SQLException;
import java.text.MessageFormat;

@Slf4j
@Configuration
public class KafkaConfiguration {

    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) ->
                        log.error(
                            MessageFormat.format( "Error occurred during {0} event processing: ", consumerRecord.topic()),
                            exception
                        ),
                new FixedBackOff(3000, 3)
        );
        errorHandler.addNotRetryableExceptions(SQLException.class);
        return errorHandler;
    }

}
