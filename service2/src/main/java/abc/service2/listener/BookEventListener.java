package abc.service2.listener;

import abc.service2.model.event.BookEvent;
import abc.service2.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventListener {

    private final BookService bookService;

    @KafkaListener(topics = "book_created", groupId = "${spring.kafka.consumer.group-id}")
    public void listenCreated(BookEvent bookEvent) {
        log.info("Received book_created event: {}", bookEvent);
        bookService.create(bookEvent);
    }

    @KafkaListener(topics = "book_rented", groupId = "${spring.kafka.consumer.group-id}")
    public void listenRented(BookEvent bookEvent) {
        log.info("Received book_rented event: {}", bookEvent);
        bookService.update(bookEvent);
    }

}
