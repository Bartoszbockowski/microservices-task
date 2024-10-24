package abc.service2.mapper;

import abc.service2.model.Book;
import abc.service2.model.dto.BookDto;
import abc.service2.model.event.BookEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BookMapperTest {

    @Test
    void testMapToDto_HappyPath_ResultInBookDtoBeingReturned() {
        // given
        Book book = Book.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .build();

        // when
        BookDto bookDto = BookMapper.mapToDto(book);

        // then
        assertEquals(bookDto.getIsbn(), book.getIsbn());
        assertEquals(bookDto.getTitle(), book.getTitle());
        assertEquals(bookDto.getAuthor(), book.getAuthor());
        assertEquals(bookDto.getGenre(), book.getGenre());
    }

    @Test
    void testFromEvent_HappyPath_ResultInBookBeingReturned() {
        // given
        BookEvent bookEvent = BookEvent.builder()
                .isbn("12345678910")
                .title("Title")
                .author("Author")
                .genre("Genre")
                .build();

        // when
        Book book = BookMapper.fromEvent(bookEvent);

        // then
        assertEquals(book.getIsbn(), bookEvent.getIsbn());
        assertEquals(book.getTitle(), bookEvent.getTitle());
        assertEquals(book.getAuthor(), bookEvent.getAuthor());
        assertEquals(book.getGenre(), bookEvent.getGenre());
    }
}

