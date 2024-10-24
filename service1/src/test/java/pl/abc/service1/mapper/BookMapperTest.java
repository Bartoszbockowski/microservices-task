package pl.abc.service1.mapper;

import org.junit.jupiter.api.Test;
import pl.abc.service1.model.Book;
import pl.abc.service1.model.command.CreateBookCommand;
import pl.abc.service1.model.dto.BookDto;

import static org.junit.jupiter.api.Assertions.*;

class BookMapperTest {

    @Test
    void testFromCommand_HappyPath_ResultInBookBeingReturned() {
        // given
        CreateBookCommand createBookCommand = new CreateBookCommand();
        createBookCommand.setIsbn("12345678910");
        createBookCommand.setTitle("Title");
        createBookCommand.setAuthor("Author");
        createBookCommand.setGenre("Genre");

        // when
        Book book = BookMapper.fromCommand(createBookCommand);

        // then
        assertEquals(createBookCommand.getIsbn(), book.getIsbn());
        assertEquals(createBookCommand.getTitle(), book.getTitle());
        assertEquals(createBookCommand.getAuthor(), book.getAuthor());
        assertEquals(createBookCommand.getGenre(), book.getGenre());
        assertNull(book.getPerson());
    }

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

}