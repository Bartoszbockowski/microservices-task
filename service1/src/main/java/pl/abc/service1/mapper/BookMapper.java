package pl.abc.service1.mapper;

import pl.abc.service1.model.Book;
import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.dto.BookDto;
import pl.abc.service1.model.event.BookEvent;

public class BookMapper {

    public static Book fromCommand(CreateBookCommand command) {
        return Book.builder()
                .isbn(command.getIsbn())
                .title(command.getTitle())
                .author(command.getAuthor())
                .genre(command.getGenre())
                .person(command.getPerson())
                .build();
    }

    public static BookDto mapToDto(Book book) {
        return BookDto.builder()
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .person(book.getPerson())
                .version(book.getVersion())
                .build();
    }

    public static BookEvent mapToEvent(Book book) {
        return BookEvent.builder()
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .genre(book.getGenre())
                .person(book.getPerson())
                .version(book.getVersion())
                .build();
    }

}
