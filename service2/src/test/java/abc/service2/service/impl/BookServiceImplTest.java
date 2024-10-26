package abc.service2.service.impl;

import abc.service2.container.KafkaTestContainer;
import abc.service2.listener.BookEventListener;
import abc.service2.model.Book;
import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;
import abc.service2.repository.BookRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
@Sql(scripts = "classpath:cleanup-all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BookServiceImplTest implements KafkaTestContainer {

    @SpyBean
    private BookServiceImpl bookService;

    @Autowired
    private KafkaTemplate<String, BookEvent> kafkaTemplate;

    @SpyBean
    private BookRepository bookRepository;

    @SpyBean
    private BookEventListener bookEventListener;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        assertTrue(KafkaTestContainer.container.isRunning());
    }

    @AfterAll
    static void tearDown() {
        KafkaTestContainer.container.stop();
    }

    @Test
    void testFindAll_HappyPath_ResultInBookDtosBeingReturned() {
        // given
        List<Book> books = List.of(
                Book.builder()
                        .isbn("12345678910")
                        .title("Title")
                        .author("Author")
                        .genre("Genre")
                        .person("Thomas")
                        .build(),
                Book.builder()
                        .isbn("12345678911")
                        .title("Title1")
                        .author("Author1")
                        .genre("Genre1")
                        .person("Thomas1")
                        .build()
        );
        bookRepository.saveAll(books);

        SessionFactory session = entityManagerFactory.unwrap(SessionFactory.class);
        session.getStatistics().clear();
        session.getStatistics().setStatisticsEnabled(true);

        // when
        List<BookDto> bookDtos = bookService.findAll();

        // then
        long queryCount = session.getStatistics().getQueryExecutionCount();
        assertEquals(1, queryCount);
        assertEquals(bookDtos.size(), 2);

        verify(bookRepository).findAllByPersonNotNull();
    }

    @Test
    void testCreate_HappyPath_ResultInBookBeingSaved() {
        // given
        BookEvent event = BookEvent.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Thomas")
                .version(1)
                .build();

        SessionFactory session = entityManagerFactory.unwrap(SessionFactory.class);
        session.getStatistics().clear();
        session.getStatistics().setStatisticsEnabled(true);

        // when
        assertFalse(bookRepository.existsById(event.getIsbn()));
        kafkaTemplate.send("book_created", event);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> verify(bookEventListener).listenCreated(any(BookEvent.class)));

        long queryCount = session.getStatistics().getQueryExecutionCount();
        assertEquals(1, queryCount);

        Book book = bookRepository.findById(event.getIsbn()).get();
        assertEquals(book.getTitle(), event.getTitle());
        assertEquals(book.getIsbn(), event.getIsbn());
        assertEquals(book.getVersion(), event.getVersion());
        assertEquals(book.getAuthor(), event.getAuthor());
        assertEquals(book.getGenre(), event.getGenre());
        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void testCreate_AlreadyExists_ResultInBookNotBeingSaved() {
        // given
        BookEvent event = BookEvent.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Thomas")
                .version(1)
                .build();
        bookRepository.save(Book.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Thomas")
                .version(1)
                .build());

        SessionFactory session = entityManagerFactory.unwrap(SessionFactory.class);
        session.getStatistics().clear();
        session.getStatistics().setStatisticsEnabled(true);

        // when
        assertEquals(bookRepository.findAll().size(), 1);
        kafkaTemplate.send("book_created", event);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> verify(bookEventListener).listenCreated(any(BookEvent.class)));
        assertEquals(bookRepository.findAll().size(), 1);
    }

    @Test
    void testUpdate_HappyPath_ResultInBookBeingUpdated() {
        // given
        BookEvent event = BookEvent.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Thomas")
                .version(2)
                .build();
        bookRepository.save(Book.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .version(1)
                .build());

        // when
        Book book = bookRepository.findById(event.getIsbn()).get();
        assertNull(book.getPerson());
        kafkaTemplate.send("book_rented", event);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> {
                    Book updatedBook = bookRepository.findById(event.getIsbn()).get();
                    assertEquals(updatedBook.getPerson(), event.getPerson());
                    assertEquals(updatedBook.getVersion(), event.getVersion());
                });
        verify(bookEventListener).listenRented(any(BookEvent.class));
    }
}