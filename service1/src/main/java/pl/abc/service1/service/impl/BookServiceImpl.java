package pl.abc.service1.service.impl;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pl.abc.service1.exception.BookAlreadyExistsException;
import pl.abc.service1.exception.BookAlreadyRentedException;
import pl.abc.service1.mapper.BookMapper;
import pl.abc.service1.model.Book;
import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.command.RentBookCommand;
import pl.abc.service1.model.dto.BookDto;
import pl.abc.service1.sender.BookSender;
import pl.abc.service1.service.BookService;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final MongoTemplate mongoTemplate;
    private final BookSender bookSender;

    @Override
    public List<BookDto> findAll() {
        return mongoTemplate.findAll(Book.class)
                .stream()
                .map(BookMapper::mapToDto)
                .toList();
    }

    @Override
    public BookDto create(CreateBookCommand command) {
        if (existsByIsbn(command.getIsbn())) {
            throw new BookAlreadyExistsException(MessageFormat
                    .format("Book with given isbn={0} already exists.", command.getIsbn()));
        }
        Book book = BookMapper.fromCommand(command);
        mongoTemplate.save(book);

        bookSender.send("book_created", BookMapper.mapToEvent(book));
        return BookMapper.mapToDto(book);
    }

    @Override
    public BookDto rent(RentBookCommand command) {
        Query query = new Query(Criteria.where("isbn").is(command.getIsbn())
                .andOperator(Criteria.where("person").exists(false)
                        .orOperator(Criteria.where("person").is(null))));
        Update update = new Update().set("person", command.getClientName());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Book.class);

        if (result.getMatchedCount() != 1) {
            throw new BookAlreadyRentedException(MessageFormat
                    .format("Book with given isbn={0} is already rented.", command.getIsbn()));
        }

        Book book = mongoTemplate.findOne(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class);
        BookDto dto = Optional.ofNullable(book)
                .map(BookMapper::mapToDto)
                .orElseThrow(() -> new BookAlreadyRentedException(MessageFormat
                    .format("Book with given isbn={0} is already rented.", command.getIsbn())));
        bookSender.send("book_rented", BookMapper.mapToEvent(book));
        return dto;
    }

    private boolean existsByIsbn(String isbn) {
        return mongoTemplate.exists(new Query(Criteria.where("isbn").is(isbn)), Book.class);
    }
}
