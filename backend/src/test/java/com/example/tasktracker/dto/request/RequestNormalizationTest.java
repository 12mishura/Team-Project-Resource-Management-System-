package com.example.tasktracker.dto.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class RequestNormalizationTest {
    private Locale originalLocale;

    @BeforeEach
    void useTurkishLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void registerEmailIsLowercasedIndependentlyOfLocale() {
        RegisterUserRequest request = new RegisterUserRequest(" ILIA@MAIL.COM ", " ilia ", "password123");

        assertThat(request.email()).isEqualTo("ilia@mail.com");
        assertThat(request.username()).isEqualTo("ilia");
    }

    @Test
    void loginEmailIsLowercasedIndependentlyOfLocale() {
        assertThat(new LoginUserRequest(" ILIA@MAIL.COM ", "x").email()).isEqualTo("ilia@mail.com");
    }

    @Test
    void inviteCodeIsTrimmedAndLowercased() {
        assertThat(new JoinOrganizationRequest("  IJKL-2345 ").inviteCode()).isEqualTo("ijkl-2345");
    }

    @Test
    void titlesAreTrimmed() {
        assertThat(new CreateOrganizationRequest("  My Title  ", null).title()).isEqualTo("My Title");
        assertThat(new UpdateOrganizationRequest("  My Title  ", null).title()).isEqualTo("My Title");
    }

    @Test
    void nullValuesStayNull() {
        assertThat(new RegisterUserRequest(null, null, null).email()).isNull();
        assertThat(new LoginUserRequest(null, null).email()).isNull();
        assertThat(new JoinOrganizationRequest(null).inviteCode()).isNull();
        assertThat(new CreateOrganizationRequest(null, null).title()).isNull();
        assertThat(new UpdateOrganizationRequest(null, null).title()).isNull();
    }
}
