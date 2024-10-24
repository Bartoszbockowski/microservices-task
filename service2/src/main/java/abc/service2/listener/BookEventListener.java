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

    @KafkaListener(topics = "book_created", groupId = "book-service-2")
    public void listenCreated(BookEvent bookEvent) {
        log.info("Received book_create event: {}", bookEvent);
        try {
            bookService.create(bookEvent);
        } catch (Exception e) {
            log.error("Error processing book_created event", e);
        }
    }

    @KafkaListener(topics = "book_rented", groupId = "book-service-2")
    public void listenRented(BookEvent bookEvent) {
        log.info("Received book_rented event: {}", bookEvent);
        try {
            bookService.update(bookEvent);
        } catch (Exception e) {
            log.error("Error processing book_rented event", e);
        }
    }

}
