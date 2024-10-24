package pl.abc.service1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.command.RentBookCommand;
import pl.abc.service1.model.dto.BookDto;
import pl.abc.service1.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookDto> findAll() {
        return bookService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookDto create(@RequestBody @Valid CreateBookCommand command) {
        return bookService.create(command);
    }

    @PutMapping
    public BookDto rent(@Valid RentBookCommand command) {
        return bookService.rent(command);
    }
}
