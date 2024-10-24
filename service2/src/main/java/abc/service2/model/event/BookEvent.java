package abc.service2.model.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookEvent {

    public String isbn;
    private String title;
    private String author;
    private String genre;
    private String person;
    private long version;
}
