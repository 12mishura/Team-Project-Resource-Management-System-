package com.example.tasktracker.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxBytesValidator.class)
public @interface MaxBytes {
    int value();

    String message() default "must not exceed {value} bytes in UTF-8";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
