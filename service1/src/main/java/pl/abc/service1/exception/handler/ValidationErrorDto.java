package pl.abc.service1.exception.handler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ValidationErrorDto extends ExceptionDto {

    private static final String EXCEPTION_MESSAGE = "violation error";
    private final List<ViolationInfo> violations = new ArrayList<>();

    public ValidationErrorDto() {
        super(EXCEPTION_MESSAGE);
    }

    public void addViolation(String field, String message) {
        violations.add(new ViolationInfo(field, message));
    }

    public record ViolationInfo(String field, String message) {}
}
