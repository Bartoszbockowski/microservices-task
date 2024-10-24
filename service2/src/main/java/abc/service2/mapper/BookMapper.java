package abc.service2.mapper;

import abc.service2.model.Book;
import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;

public class BookMapper {

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

    public static Book fromEvent(BookEvent bookEvent) {
        return Book.builder()
                .isbn(bookEvent.getIsbn())
                .title(bookEvent.getTitle())
                .author(bookEvent.getAuthor())
                .genre(bookEvent.getGenre())
                .person(bookEvent.getPerson())
                .version(bookEvent.getVersion())
                .build();
    }
}
