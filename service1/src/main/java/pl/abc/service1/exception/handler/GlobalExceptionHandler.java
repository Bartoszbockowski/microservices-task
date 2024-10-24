package pl.abc.service1.exception.handler;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.abc.service1.exception.BookAlreadyExistsException;
import pl.abc.service1.exception.BookAlreadyRentedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BookAlreadyExistsException.class, BookAlreadyRentedException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleBadRequestExceptions(Exception e) {
        return new ExceptionDto(e.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionDto handleDuplicateKeyException() {
        return new ExceptionDto("An error occurred while attempting to persist object. Please validate given data.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorDto handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        ValidationErrorDto errorDto = new ValidationErrorDto();
        e.getFieldErrors().forEach(error -> errorDto.addViolation(error.getField(), error.getDefaultMessage()));
        return errorDto;
    }
}
