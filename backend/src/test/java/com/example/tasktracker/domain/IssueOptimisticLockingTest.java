package com.example.tasktracker.domain;

import com.example.tasktracker.AbstractIntegrationTest;
import com.example.tasktracker.repository.IssueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueOptimisticLockingTest extends AbstractIntegrationTest {

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void staleIssueCannotOverwriteNewerChanges() throws Exception {
        String token = registerAndLogin("owner");
        String organizationId = createOrganization(token, "MyOrganization");
        String projectId = read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Backend Rewrite\"}"))
                .andReturn(), "$.id");
        String path = "/organizations/" + organizationId + "/projects/" + projectId + "/issues";
        UUID issueId = UUID.fromString(read(mockMvc.perform(withToken(post(path), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TASK\",\"title\":\"Shared task\"}"))
                .andReturn(), "$.id"));

        Issue stale = transactionTemplate.execute(status -> issueRepository.findById(issueId).orElseThrow());

        mockMvc.perform(withToken(patch(path + "/" + issueId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fresh title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        stale.setTitle("Stale title");
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> issueRepository.saveAndFlush(stale)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM issues WHERE id = ?", String.class, issueId))
                .isEqualTo("Fresh title");
    }
}
