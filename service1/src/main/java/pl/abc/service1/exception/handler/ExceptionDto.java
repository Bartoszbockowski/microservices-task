package pl.abc.service1.exception.handler;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.ZonedDateTime;

@Getter
@RequiredArgsConstructor
public class ExceptionDto {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ssXXX", timezone = "Europe/Warsaw")
    private final ZonedDateTime timestamp = ZonedDateTime.now();
    private final String message;
}
