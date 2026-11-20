package com.example.tasktracker.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxBytesValidatorTest {
    private static Validator validator;

    private record Sample(@MaxBytes(4) String value) {
    }

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void nullIsValid() {
        assertThat(validator.validate(new Sample(null))).isEmpty();
    }

    @Test
    void asciiWithinLimitIsValid() {
        assertThat(validator.validate(new Sample("abcd"))).isEmpty();
    }

    @Test
    void asciiOverLimitIsInvalid() {
        assertThat(validator.validate(new Sample("abcde"))).hasSize(1);
    }

    @Test
    void multibyteCharactersAreCountedInBytes() {
        assertThat(validator.validate(new Sample("жж"))).isEmpty();
        assertThat(validator.validate(new Sample("жжж"))).hasSize(1);
    }

    @Test
    void messageContainsLimit() {
        assertThat(validator.validate(new Sample("abcde")))
                .extracting(violation -> violation.getMessage())
                .containsExactly("must not exceed 4 bytes in UTF-8");
    }
}
