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

        BookDto dto = BookMapper.mapToDto(book);
        bookSender.send("book_created", dto);
        return dto;
    }

    @Override
    public BookDto rent(RentBookCommand command) {
        Query query = new Query(Criteria.where("isbn").is(command.getIsbn())
                .andOperator(Criteria.where("person").exists(false)
                        .orOperator(Criteria.where("person").is(null))));
        Update update = new Update().set("person", command.getClientName());
        UpdateResult result = mongoTemplate.updateFirst(query, update, Book.class);

        Book book = null;
        if (result.getMatchedCount() == 1) {
            book = mongoTemplate.findOne(new Query(Criteria.where("isbn").is(command.getIsbn())), Book.class);
        }
        if (book == null) {
            throw new BookAlreadyRentedException(MessageFormat
                    .format("Book with given isbn={0} is already rented.", command.getIsbn()));
        }

        BookDto dto = BookMapper.mapToDto(book);
        bookSender.send("book_rented", dto);
        return dto;
    }

    private boolean existsByIsbn(String isbn) {
        return mongoTemplate.exists(new Query(Criteria.where("isbn").is(isbn)), Book.class);
    }
}
