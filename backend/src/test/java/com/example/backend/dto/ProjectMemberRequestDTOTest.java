package com.example.backend.dto;

import com.example.backend.entity.enums.ProjectRole;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMemberRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void validRequest_hasNoViolations() {
        var dto = new ProjectMemberRequest(1, 2, ProjectRole.MEMBER);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void nullUserId_isRejected() {
        var dto = new ProjectMemberRequest(null, 2, ProjectRole.MEMBER);

        Set<ConstraintViolation<ProjectMemberRequest>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("userId", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void nullProjectId_isRejected() {
        var dto = new ProjectMemberRequest(1, null, ProjectRole.MEMBER);

        assertEquals(1, validator.validate(dto).size());
    }

    @Test
    void nullRole_isRejected() {
        var dto = new ProjectMemberRequest(1, 2, null);

        assertEquals(1, validator.validate(dto).size());
    }

    @Test
    void allFieldsNull_producesThreeViolations() {
        var dto = new ProjectMemberRequest(null, null, null);

        assertEquals(3, validator.validate(dto).size());
    }
}