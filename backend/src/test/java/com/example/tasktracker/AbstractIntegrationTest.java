package com.example.tasktracker;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.UnsupportedEncodingException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jwt.secret=IvkfLEIc3N4UbynZUaCUqbbzwEpQvk7UcV7wljcAIiy6HUJaqVvfZkYUJXWTv0yx")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens, issue_events, issue_comments, issues, project_members, projects, organization_members, organization, users CASCADE");
    }

    protected ResultActions register(String email, String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","username":"%s","password":"%s"}
                        """.formatted(email, username, password)));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    protected String registerAndLogin(String username) throws Exception {
        String email = username + "@mail.com";
        register(email, username, PASSWORD).andExpect(status().isCreated());
        MvcResult result = login(email, PASSWORD).andExpect(status().isOk()).andReturn();
        return read(result, "$.accessToken");
    }

    protected String createOrganization(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(withToken(post("/organizations"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"}
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, "$.id");
    }

    protected String getInviteCode(String token, String organizationId) throws Exception {
        MvcResult result = mockMvc.perform(withToken(get("/organizations/{id}/invite-code", organizationId), token))
                .andExpect(status().isOk())
                .andReturn();
        return read(result, "$.inviteCode");
    }

    protected ResultActions join(String token, String inviteCode) throws Exception {
        return mockMvc.perform(withToken(post("/organizations/join"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"inviteCode":"%s"}
                        """.formatted(inviteCode)));
    }

    protected String joinAndGetMemberId(String token, String inviteCode) throws Exception {
        MvcResult result = join(token, inviteCode).andExpect(status().isCreated()).andReturn();
        return read(result, "$.id");
    }

    protected ResultActions changeRole(String token, String organizationId, String memberId, String role) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{organizationId}/members/{memberId}", organizationId, memberId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"role":"%s"}
                        """.formatted(role)));
    }

    protected ResultActions removeMember(String token, String organizationId, String memberId) throws Exception {
        return mockMvc.perform(withToken(delete("/organizations/{organizationId}/members/{memberId}", organizationId, memberId), token));
    }

    protected ResultActions leave(String token, String organizationId) throws Exception {
        return mockMvc.perform(withToken(delete("/organizations/{organizationId}/members/me", organizationId), token));
    }

    protected ResultActions getMembers(String token, String organizationId) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{organizationId}/members", organizationId), token));
    }

    protected MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected String read(MvcResult result, String path) throws UnsupportedEncodingException {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
