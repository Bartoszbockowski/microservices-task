package pl.abc.service1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import pl.abc.service1.containers.KafkaTestContainer;
import pl.abc.service1.containers.MongoDBTestContainer;
import pl.abc.service1.listenter.TestKafkaListener;
import pl.abc.service1.model.Book;
import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.command.RentBookCommand;
import pl.abc.service1.model.event.BookEvent;
import pl.abc.service1.sender.BookSender;
import pl.abc.service1.service.BookService;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;


@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class BookControllerTest implements MongoDBTestContainer, KafkaTestContainer {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @SpyBean
    private BookService bookService;

    @SpyBean
    private MongoTemplate mongoTemplate;

    @SpyBean
    private BookSender bookSender;

    private final static String URL = "/api/v1/book";

    @BeforeEach
    void setUp() {
        assertTrue(MongoDBTestContainer.container.isRunning());
        assertTrue(KafkaTestContainer.container.isRunning());
        mongoTemplate.remove(new Query(), Book.class);
        testKafkaListener.getCreated().clear();
        testKafkaListener.getRented().clear();
    }

    @AfterAll
    static void tearDown() {
        KafkaTestContainer.container.stop();
    }

    // create
    @Test
    void BookController_Create_HappyPath_WithPerson_ResultInBookBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");

        assertFalse(mongoTemplate.exists(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class));

        // when, then
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isbn").value(command.getIsbn()))
            .andExpect(jsonPath("$.title").value(command.getTitle()))
            .andExpect(jsonPath("$.author").value(command.getAuthor()))
            .andExpect(jsonPath("$.genre").value(command.getGenre()))
            .andExpect(jsonPath("$.person").value(command.getPerson()))
            .andExpect(jsonPath("$.version").value(1));

        assertTrue(mongoTemplate.exists(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class));
        await().atMost(Duration.ofSeconds(3))
                .until(() -> !testKafkaListener.getCreated().isEmpty());
        assertEquals(testKafkaListener.getCreated().size(), 1);
        verify(bookService).create(command);
    }

    @Test
    void BookController_Create_HappyPath_WithoutPerson_ResultInBookBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");

        assertFalse(mongoTemplate.exists(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class));

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value(command.getIsbn()))
                .andExpect(jsonPath("$.title").value(command.getTitle()))
                .andExpect(jsonPath("$.author").value(command.getAuthor()))
                .andExpect(jsonPath("$.genre").value(command.getGenre()))
                .andExpect(jsonPath("$.person").isEmpty())
                .andExpect(jsonPath("$.version").value(1));

        assertTrue(mongoTemplate.exists(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class));
        await().atMost(Duration.ofSeconds(3))
                .until(() -> !testKafkaListener.getCreated().isEmpty());
        assertEquals(testKafkaListener.getCreated().size(), 1);
        verify(bookService).create(command);
    }

    @Test
    void BookController_Create_Isbn_RaceCondition_ResultInOneBookBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        ExecutorService executor = Executors.newFixedThreadPool(2);
        MockMvc mvc1 = webAppContextSetup(webApplicationContext).build();
        MockMvc mvc2 = webAppContextSetup(webApplicationContext).build();
        Callable<MvcResult> createBookTask1 = () -> mvc1.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(command)))
                .andReturn();
        Callable<MvcResult> createBookTask2 = () -> mvc2.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andReturn();

        List<Future<MvcResult>> results = executor.invokeAll(List.of(createBookTask1, createBookTask2));
        executor.shutdown();

        MvcResult res1 = results.get(0).get();
        MvcResult res2 = results.get(1).get();
        JsonNode result1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        JsonNode result2 = objectMapper.readTree(res2.getResponse().getContentAsString());

        String message = "An error occurred while attempting to persist object. Please validate given data.";

        if (res1.getResponse().getStatus() == HttpStatus.CREATED.value()) {
            assertEquals(result1.get("isbn").asText(), command.getIsbn());
            assertEquals(result1.get("title").asText(), command.getTitle());
            assertEquals(result1.get("author").asText(), command.getAuthor());
            assertEquals(result1.get("genre").asText(), command.getGenre());
            assertEquals(result1.get("person").asText(), command.getPerson());
            assertEquals(result1.get("version").asLong(), 1);
        } else {
            assertEquals(res1.getResponse().getStatus(), HttpStatus.BAD_REQUEST.value());
            assertFalse(result1.has("isbn"));
            assertFalse(result1.has("title"));
            assertFalse(result1.has("author"));
            assertFalse(result1.has("genre"));
            assertFalse(result1.has("person"));
            assertFalse(result1.has("version"));
            assertTrue(result1.has("timestamp"));
            assertEquals(result1.get("message").asText(), message);
        }

        if (res2.getResponse().getStatus() == HttpStatus.CREATED.value()) {
            assertEquals(result2.get("isbn").asText(), command.getIsbn());
            assertEquals(result2.get("title").asText(), command.getTitle());
            assertEquals(result2.get("author").asText(), command.getAuthor());
            assertEquals(result2.get("genre").asText(), command.getGenre());
            assertEquals(result2.get("person").asText(), command.getPerson());
            assertEquals(result2.get("version").asLong(), 1);
        } else {
            assertEquals(res2.getResponse().getStatus(), HttpStatus.BAD_REQUEST.value());
            assertFalse(result2.has("isbn"));
            assertFalse(result2.has("title"));
            assertFalse(result2.has("author"));
            assertFalse(result2.has("genre"));
            assertFalse(result2.has("person"));
            assertFalse(result2.has("version"));
            assertTrue(result2.has("timestamp"));
            assertEquals(result2.get("message").asText(), message);
        }

        assertEquals(mongoTemplate.estimatedCount(Book.class), 1);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> !testKafkaListener.getCreated().isEmpty());
        assertEquals(testKafkaListener.getCreated().size(), 1);

        verify(bookService, times(2)).create(command);
        verify(bookSender).send(eq("book_created"), any(BookEvent.class));
    }

    @Test
    void BookController_Create_Isbn_BlankValue_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");
        String message = "BLANK_VALUE";
        String field = "isbn";

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);
        verifyNoInteractions(bookService);
    }

    @Test
    void BookController_Create_Isbn_WrongRange_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("123");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");
        String message = "ISBN_CHARACTER_RANGE_IS_10-13";
        String field = "isbn";

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);
        verifyNoInteractions(bookService);
    }

    @Test
    void BookController_Create_Isbn_AlreadyExists_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");
        mongoTemplate.save(Book.builder()
                .isbn(command.getIsbn())
                .title(command.getTitle())
                .author(command.getAuthor())
                .genre(command.getGenre())
                .person(command.getPerson())
                .build());
        String message = MessageFormat
                .format("Book with given isbn={0} already exists.", command.getIsbn());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 1);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 1);
        verify(bookService).create(command);
        verifyNoInteractions(bookSender);
    }

    @Test
    void BookController_Create_Title_BlankValue_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setAuthor("Author");
        command.setGenre("Genre");
        command.setPerson("Thomas");
        String message = "BLANK_VALUE";
        String field = "title";

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);
        verifyNoInteractions(bookService);
    }

    @Test
    void BookController_Create_Author_BlankValue_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setGenre("Genre");
        command.setPerson("Thomas");
        String message = "BLANK_VALUE";
        String field = "author";

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);
        verifyNoInteractions(bookService);
    }

    @Test
    void BookController_Create_Genre_BlankValue_ResultInBookNotBeingSaved() throws Exception {
        // given
        CreateBookCommand command = new CreateBookCommand();
        command.setIsbn("12345678910");
        command.setTitle("Title");
        command.setAuthor("Author");
        command.setPerson("Thomas");
        String message = "BLANK_VALUE";
        String field = "genre";

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);

        // when, then
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.estimatedCount(Book.class), 0);
        verifyNoInteractions(bookService);
    }

    // rent
    @Test
    void BookController_Rent_HappyPath_ResultInBookBeingUpdated() throws Exception {
        // given
        String isbn = "12345678910";
        Book book = Book.builder()
                .isbn(isbn)
                .title("Title")
                .author("Author")
                .genre("Genre")
                .build();
        mongoTemplate.save(book);
        book = mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class);
        RentBookCommand command = new RentBookCommand();
        command.setIsbn(isbn);
        command.setClientName("Thomas");

        assertFalse(mongoTemplate.exists(new Query(Criteria.where("person").exists(true)), Book.class));

        // when, then
        mockMvc.perform(put(URL)
                        .param("isbn", command.getIsbn())
                        .param("clientName", command.getClientName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(book.getIsbn()))
                .andExpect(jsonPath("$.title").value(book.getTitle()))
                .andExpect(jsonPath("$.author").value(book.getAuthor()))
                .andExpect(jsonPath("$.genre").value(book.getGenre()))
                .andExpect(jsonPath("$.person").value(command.getClientName()))
                .andExpect(jsonPath("$.version").value(book.getVersion() + 1));

        assertTrue(mongoTemplate.exists(new Query(Criteria.where("person").exists(true)), Book.class));
        await().atMost(Duration.ofSeconds(3))
                .until(() -> !testKafkaListener.getRented().isEmpty());
        assertEquals(testKafkaListener.getRented().size(), 1);
        verify(bookService).rent(command);
    }

    @Test
    void BookController_Rent_AlreadyRented_ResultInBookNotBeingUpdated() throws Exception {
        // given
        String isbn = "12345678910";
        Book book = Book.builder()
                .isbn(isbn)
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Fredrick")
                .build();
        mongoTemplate.save(book);
        RentBookCommand command = new RentBookCommand();
        command.setIsbn(isbn);
        command.setClientName("Thomas");
        String message = MessageFormat
                .format("Book with given isbn={0} is already rented.", command.getIsbn());

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());

        // when, then
        mockMvc.perform(put(URL)
                        .param("isbn", command.getIsbn())
                        .param("clientName", command.getClientName()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());
        verify(bookService).rent(command);
        verifyNoInteractions(bookSender);
    }

    @Test
    void BookController_Rent_Isbn_BlankValue_ResultInBookNotBeingUpdated() throws Exception {
        // given
        String isbn = "12345678910";
        Book book = Book.builder()
                .isbn(isbn)
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Fredrick")
                .build();
        mongoTemplate.save(book);
        RentBookCommand command = new RentBookCommand();
        command.setClientName("Thomas");
        String message = "BLANK_VALUE";
        String field = "isbn";

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());

        // when, then
        mockMvc.perform(put(URL)
                        .param("isbn", command.getIsbn())
                        .param("clientName", command.getClientName()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());
        verifyNoInteractions(bookService);
    }

    @Test
    void BookController_Rent_Isbn_WrongRange_ResultInBookNotBeingUpdated() throws Exception {
        // given
        String isbn = "12345678910";
        Book book = Book.builder()
                .isbn(isbn)
                .title("Title")
                .author("Author")
                .genre("Genre")
                .person("Fredrick")
                .build();
        mongoTemplate.save(book);
        RentBookCommand command = new RentBookCommand();
        command.setClientName("Thomas");
        command.setIsbn("1");
        String message = "ISBN_CHARACTER_RANGE_IS_10-13";
        String field = "isbn";

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());

        // when, then
        mockMvc.perform(put(URL)
                        .param("isbn", command.getIsbn())
                        .param("clientName", command.getClientName()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.violations[0].message").value(message))
                .andExpect(jsonPath("$.violations[0].field").value(field))
                .andExpect(jsonPath("$.isbn").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.genre").doesNotExist())
                .andExpect(jsonPath("$.person").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());

        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getVersion(), 1);
        assertEquals(mongoTemplate.findOne(new Query(Criteria.where("isbn").is(isbn)), Book.class).getPerson(), book.getPerson());
        verifyNoInteractions(bookService);
    }

    // find all
    @Test
    void BookController_FindAll_HappyPath_ResultInBookDtosBeingReturned() throws Exception {
        for (int i = 0; i < 10; i++) {
            Book book = Book.builder()
                    .isbn("1234567891" + i)
                    .title("Title" + i)
                    .author("Author" + i)
                    .genre("Genre" + i)
                    .build();
            mongoTemplate.save(book);
        }

        // when, then
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[7].isbn").value("12345678917"))
                .andExpect(jsonPath("$[7].title").value("Title7"))
                .andExpect(jsonPath("$[7].author").value("Author7"))
                .andExpect(jsonPath("$[7].genre").value("Genre7"));
    }
}