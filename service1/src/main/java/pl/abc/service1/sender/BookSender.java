package pl.abc.service1.sender;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pl.abc.service1.model.dto.BookDto;

@Component
@RequiredArgsConstructor
public class BookSender {

    private final KafkaTemplate<String, BookDto> kafkaTemplate;

    public void send(String topic, BookDto bookDto) {
        kafkaTemplate.send(topic, bookDto);
    }
}
