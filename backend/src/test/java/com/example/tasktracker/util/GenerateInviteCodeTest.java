package com.example.tasktracker.util;

import com.example.tasktracker.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GenerateInviteCodeTest {

    @Test
    void alphabetHasNoDuplicateCharacters() throws Exception {
        Field field = GenerateInviteCode.class.getDeclaredField("ALPHABET");
        field.setAccessible(true);
        String alphabet = (String) field.get(null);

        assertThat(alphabet.chars().distinct().count()).isEqualTo(alphabet.length());
    }

    @Test
    void generatedCodeHasExpectedFormat() {
        GenerateInviteCode generator = new GenerateInviteCode(mock(OrganizationRepository.class));

        for (int i = 0; i < 1000; i++) {
            assertThat(generator.generateCode()).matches("^[a-z0-9]{4}-[a-z0-9]{4}$");
        }
    }

    @Test
    void getCodeFailsAfterTooManyCollisions() {
        OrganizationRepository repository = mock(OrganizationRepository.class);
        when(repository.existsByInviteCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> new GenerateInviteCode(repository).getCode())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to generate unique invite code");
    }
}
