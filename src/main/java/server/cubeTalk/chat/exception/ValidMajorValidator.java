package server.cubeTalk.chat.exception;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

public class ValidMajorValidator implements ConstraintValidator<ValidMajor, String> {

    private List<String> allowedWords;

    @Override
    public void initialize(ValidMajor constraintAnnotation) {
        allowedWords = Arrays.asList(constraintAnnotation.word());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return allowedWords.contains(value);
    }
}
