package pl.abc.service1.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@Document(collection = "books")
public class Book {

    @Id
    private String isbn;
    private String title;
    private String author;
    private String genre;
    private String person;

    @Version
    private long version;
}
