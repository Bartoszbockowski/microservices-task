package pl.abc.service1.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.abc.service1.model.event.BookEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookSender {

    private final KafkaTemplate<String, BookEvent> kafkaTemplate;

    public void send(String topic, BookEvent bookEvent) {
        kafkaTemplate.send(topic, bookEvent)
                .whenComplete((record, exc) -> {
                    if (exc != null) {
                        log.error("Error sending record: ", exc);
                    }
                });
    }
}
