package pl.abc.service1.model.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RentBookCommand {

    @NotBlank(message = "BLANK_VALUE")
    private String clientName;

    @NotBlank(message = "BLANK_VALUE")
    @Size(min = 10, max = 13, message = "ISBN_CHARACTER_RANGE_IS_{min}-{max}")
    private String isbn;
}
