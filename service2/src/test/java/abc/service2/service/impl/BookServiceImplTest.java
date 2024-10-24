package abc.service2.service.impl;

import abc.service2.container.KafkaTestContainer;
import abc.service2.listener.BookEventListener;
import abc.service2.model.Book;
import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;
import abc.service2.repository.BookRepository;
import abc.service2.sender.TestKafkaSender;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
    private TestKafkaSender testKafkaSender;

    @SpyBean
    private BookRepository bookRepository;

    @SpyBean
    private EntityManager entityManager;

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
        BookDto dto = BookDto.builder()
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
        assertFalse(bookRepository.existsById(dto.getIsbn()));
        testKafkaSender.send("book_created", dto);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> verify(bookEventListener).listenCreated(any(BookEvent.class)));

        long queryCount = session.getStatistics().getQueryExecutionCount();
        assertEquals(1, queryCount);

        Book book = bookRepository.findById(dto.getIsbn()).get();
        assertEquals(book.getTitle(), dto.getTitle());
        assertEquals(book.getIsbn(), dto.getIsbn());
        assertEquals(book.getVersion(), dto.getVersion());
        assertEquals(book.getAuthor(), dto.getAuthor());
        assertEquals(book.getGenre(), dto.getGenre());
        verify(entityManager).persist(any(Book.class));
    }

    @Test
    void testCreate_AlreadyExists_ResultInBookNotBeingSaved() {
        // given
        BookDto dto = BookDto.builder()
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
        testKafkaSender.send("book_created", dto);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> verify(bookEventListener).listenCreated(any(BookEvent.class)));
        assertEquals(bookRepository.findAll().size(), 1);
    }

    @Test
    void testUpdate_HappyPath_ResultInBookBeingUpdated() {
        // given
        BookDto dto = BookDto.builder()
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
        Book book = bookRepository.findById(dto.getIsbn()).get();
        assertNull(book.getPerson());
        testKafkaSender.send("book_rented", dto);

        // then
        await().atMost(Duration.ofSeconds(61))
                .untilAsserted(() -> {
                    Book updatedBook = bookRepository.findById(dto.getIsbn()).get();
                    assertEquals(updatedBook.getPerson(), dto.getPerson());
                    assertEquals(updatedBook.getVersion(), dto.getVersion());
                });
        verify(bookEventListener).listenRented(any(BookEvent.class));
    }


}