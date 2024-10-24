package abc.service2.sender;

import abc.service2.model.dto.BookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

@Component
@ActiveProfiles("test")
@RequiredArgsConstructor
public class TestKafkaSender {

    private final KafkaTemplate<String, BookDto> kafkaTemplate;

    public void send(String topic, BookDto bookDto) {
        kafkaTemplate.send(topic, bookDto);
    }
}
