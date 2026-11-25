package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorResponseFormatTest extends AbstractIntegrationTest {
    private String token;
    private String organizationId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("alice");
        organizationId = createOrganization(token, "MyOrganization");
    }

    @Test
    void missingTokenIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(get("/organizations/{id}", organizationId)), 401, "/organizations/" + organizationId)
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void invalidTokenIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(get("/auth/me"), "abc.def.ghi")), 401, "/auth/me");
    }

    @Test
    void malformedJsonIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(post("/organizations"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{bad json")), 400, "/organizations");
    }

    @Test
    void validationMessagesStayEnglishForAnyBrowserLanguage() throws Exception {
        assertProblem(mockMvc.perform(withToken(post("/organizations"), token)
                .header("Accept-Language", "ru-RU,ru;q=0.9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")), 400, "/organizations")
                .andExpect(jsonPath("$.detail").value("title: must not be blank"));
    }

    @Test
    void invalidUuidInPathIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(get("/organizations/not-a-uuid"), token)), 400, "/organizations/not-a-uuid");
    }

    @Test
    void unsupportedMediaTypeIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(post("/organizations"), token)
                .contentType(MediaType.TEXT_PLAIN)
                .content("x")), 415, "/organizations");
    }

    @Test
    void methodNotAllowedIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(put("/organizations/{id}", organizationId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")), 405, "/organizations/" + organizationId);
    }

    @Test
    void unknownPathIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(get("/nothing"), token)), 404, "/nothing");
    }

    @Test
    void validationErrorIsProblemDetailWithFieldList() throws Exception {
        assertProblem(register("not-an-email", "al", "short"), 400, "/auth/register")
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("email:"),
                        org.hamcrest.Matchers.containsString("username:"),
                        org.hamcrest.Matchers.containsString("password:"))));
    }

    @Test
    void businessErrorIsProblemDetail() throws Exception {
        assertProblem(mockMvc.perform(withToken(post("/organizations"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"MyOrganization"}
                        """)), 409, "/organizations");
    }

    private ResultActions assertProblem(ResultActions actions, int status, String instance) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(noLegacyFields());
    }

    private ResultMatcher noLegacyFields() {
        return result -> {
            jsonPath("$.message").doesNotExist().match(result);
            jsonPath("$.error").doesNotExist().match(result);
            jsonPath("$.timestamp").doesNotExist().match(result);
        };
    }
}
