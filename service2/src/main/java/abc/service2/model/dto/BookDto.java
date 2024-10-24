package abc.service2.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookDto {

    private String isbn;
    private String title;
    private String author;
    private String genre;
    private String person;
    private long version;
}
