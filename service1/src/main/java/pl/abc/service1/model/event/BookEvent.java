package pl.abc.service1.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookEvent {

    private String isbn;
    private String title;
    private String author;
    private String genre;
    private String person;
    private long version;
}
