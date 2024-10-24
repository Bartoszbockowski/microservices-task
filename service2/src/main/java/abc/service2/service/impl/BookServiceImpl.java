package abc.service2.service.impl;

import abc.service2.exception.BookNotFoundException;
import abc.service2.mapper.BookMapper;
import abc.service2.model.Book;
import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;
import abc.service2.repository.BookRepository;
import abc.service2.service.BookService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final EntityManager entityManager;

    @Override
    public List<BookDto> findAll() {
        return bookRepository.findAllByPersonNotNull()
                .stream()
                .map(BookMapper::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public void create(BookEvent bookEvent) {
        entityManager.persist(BookMapper.fromEvent(bookEvent));
    }

    @Override
    @Transactional
    public void update(BookEvent bookEvent) {
        Book book = bookRepository.findById(bookEvent.getIsbn())
                .orElseThrow(() -> new BookNotFoundException(MessageFormat
                        .format("Book with isbn={0} not found.", bookEvent.getIsbn())));
        book.setPerson(bookEvent.getPerson());
        book.setVersion(bookEvent.getVersion());
    }
}
