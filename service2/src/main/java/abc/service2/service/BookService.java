package abc.service2.service;

import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;

import java.util.List;

public interface BookService {

    List<BookDto> findAll();

    void create(BookEvent bookEvent);

    void update(BookEvent bookEvent);
}
