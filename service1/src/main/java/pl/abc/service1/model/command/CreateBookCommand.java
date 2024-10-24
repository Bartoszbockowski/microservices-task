package pl.abc.service1.model.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBookCommand {

    @NotBlank(message = "BLANK_VALUE")
    @Size(min = 10, max = 13, message = "ISBN_CHARACTER_RANGE_IS_{min}-{max}")
    private String isbn;

    @NotBlank(message = "BLANK_VALUE")
    private String title;

    @NotBlank(message = "BLANK_VALUE")
    private String author;

    @NotBlank(message = "BLANK_VALUE")
    private String genre;

    private String person;

}
