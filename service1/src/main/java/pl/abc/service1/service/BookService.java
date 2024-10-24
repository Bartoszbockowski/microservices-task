package pl.abc.service1.service;

import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.command.RentBookCommand;
import pl.abc.service1.model.dto.BookDto;

import java.util.List;

public interface BookService {

    BookDto create(CreateBookCommand command);

    BookDto rent(RentBookCommand command);

    List<BookDto> findAll();
}
