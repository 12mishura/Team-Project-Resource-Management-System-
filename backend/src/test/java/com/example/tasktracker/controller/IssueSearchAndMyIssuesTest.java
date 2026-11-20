package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssueSearchAndMyIssuesTest extends AbstractIntegrationTest {
    private String ownerToken;
    private String devToken;
    private String otherToken;
    private String organizationId;
    private String projectId;
    private String secondOrganizationId;
    private String secondProjectId;
    private String devProjectMemberId;
    private String devSecondProjectMemberId;
    private String otherProjectMemberId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        devToken = registerAndLogin("dev");
        otherToken = registerAndLogin("other");

        organizationId = createOrganization(ownerToken, "First Organization");
        String devMemberId = joinAndGetMemberId(devToken, getInviteCode(ownerToken, organizationId));
        String otherMemberId = joinAndGetMemberId(otherToken, getInviteCode(ownerToken, organizationId));
        projectId = createProject(ownerToken, organizationId, "First Project");
        devProjectMemberId = addToProject(ownerToken, organizationId, projectId, devMemberId);
        otherProjectMemberId = addToProject(ownerToken, organizationId, projectId, otherMemberId);

        secondOrganizationId = createOrganization(ownerToken, "Second Organization");
        String devSecondMemberId = joinAndGetMemberId(devToken, getInviteCode(ownerToken, secondOrganizationId));
        secondProjectId = createProject(ownerToken, secondOrganizationId, "Second Project");
        devSecondProjectMemberId = addToProject(ownerToken, secondOrganizationId, secondProjectId, devSecondMemberId);
    }

    @Test
    void searchMatchesTitleIgnoringCaseAndTreatsWildcardsLiterally() throws Exception {
        issue(organizationId, projectId, "Fix Login page", "TASK", "LOW");
        issue(organizationId, projectId, "Logout button", "TASK", "LOW");
        issue(organizationId, projectId, "100% coverage", "TASK", "LOW");
        issue(organizationId, projectId, "snake_case names", "TASK", "LOW");
        issue(organizationId, projectId, "snakeXcase names", "TASK", "LOW");

        search("LOG").andExpect(jsonPath("$.content[*].title").value(containsInAnyOrder("Fix Login page", "Logout button")));
        search("login").andExpect(jsonPath("$.content[*].title").value(contains("Fix Login page")));
        search("100%").andExpect(jsonPath("$.content[*].title").value(contains("100% coverage")));
        search("%").andExpect(jsonPath("$.content[*].title").value(contains("100% coverage")));
        search("snake_case").andExpect(jsonPath("$.content[*].title").value(contains("snake_case names")));
        search("  ").andExpect(jsonPath("$.content", hasSize(5)));
        search("nothing-like-this").andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void sortByPriorityUsesBusinessOrderNotAlphabet() throws Exception {
        issue(organizationId, projectId, "Medium issue", "TASK", "MEDIUM");
        issue(organizationId, projectId, "Critical issue", "TASK", "CRITICAL");
        issue(organizationId, projectId, "Low issue", "TASK", "LOW");
        issue(organizationId, projectId, "High issue", "TASK", "HIGH");

        list("?sortBy=PRIORITY&direction=DESC").andExpect(jsonPath("$.content[*].title")
                .value(contains("Critical issue", "High issue", "Medium issue", "Low issue")));
        list("?sortBy=PRIORITY").andExpect(jsonPath("$.content[*].title")
                .value(contains("Low issue", "Medium issue", "High issue", "Critical issue")));
    }

    @Test
    void sortByStatusAndUpdatedAt() throws Exception {
        String first = issue(organizationId, projectId, "First issue", "TASK", "LOW");
        String second = issue(organizationId, projectId, "Second issue", "TASK", "LOW");
        issue(organizationId, projectId, "Third issue", "TASK", "LOW");
        changeStatus(second, "IN_PROGRESS");
        changeStatus(second, "IN_REVIEW");
        changeStatus(first, "IN_PROGRESS");

        list("?sortBy=STATUS&direction=DESC").andExpect(jsonPath("$.content[*].title")
                .value(contains("Second issue", "First issue", "Third issue")));
        list("?sortBy=UPDATED_AT&direction=DESC").andExpect(jsonPath("$.content[0].title").value("First issue"));
        list("?sortBy=CREATED_AT&direction=DESC").andExpect(jsonPath("$.content[0].title").value("Third issue"));
    }

    @Test
    void sortingKeepsPaginationCorrect() throws Exception {
        issue(organizationId, projectId, "Low issue", "TASK", "LOW");
        issue(organizationId, projectId, "Critical issue", "TASK", "CRITICAL");
        issue(organizationId, projectId, "High issue", "TASK", "HIGH");

        list("?sortBy=PRIORITY&direction=DESC&page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title").value(contains("Low issue")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void unknownSortValuesAreRejected() throws Exception {
        list("?sortBy=TITLE").andExpect(status().isBadRequest());
        list("?direction=SIDEWAYS").andExpect(status().isBadRequest());
    }

    @Test
    void issueResponseContainsOrganizationId() throws Exception {
        issue(organizationId, projectId, "Some issue", "TASK", "LOW");

        list("").andExpect(jsonPath("$.content[0].organizationId").value(organizationId))
                .andExpect(jsonPath("$.content[0].projectId").value(projectId));
    }

    @Test
    void myIssuesSpanAllOrganizationsAndContainOnlyMine() throws Exception {
        String mineFirst = issue(organizationId, projectId, "Mine in first org", "TASK", "HIGH");
        String mineSecond = issue(secondOrganizationId, secondProjectId, "Mine in second org", "BUG", "LOW");
        String someoneElses = issue(organizationId, projectId, "Someone elses", "TASK", "LOW");
        issue(organizationId, projectId, "Unassigned", "TASK", "LOW");
        assign(organizationId, projectId, mineFirst, devProjectMemberId);
        assign(secondOrganizationId, secondProjectId, mineSecond, devSecondProjectMemberId);
        assign(organizationId, projectId, someoneElses, otherProjectMemberId);

        myIssues(devToken, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].title").value(contains("Mine in first org", "Mine in second org")))
                .andExpect(jsonPath("$.content[*].organizationId").value(contains(organizationId, secondOrganizationId)));
        myIssues(devToken, "?type=BUG").andExpect(jsonPath("$.content[*].title").value(contains("Mine in second org")));
        myIssues(devToken, "?q=first").andExpect(jsonPath("$.content[*].title").value(contains("Mine in first org")));
        myIssues(devToken, "?sortBy=PRIORITY&direction=DESC&size=1")
                .andExpect(jsonPath("$.content[*].title").value(contains("Mine in first org")))
                .andExpect(jsonPath("$.totalElements").value(2));
        myIssues(otherToken, "").andExpect(jsonPath("$.content[*].title").value(contains("Someone elses")));
        myIssues(ownerToken, "").andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void myIssuesFollowStatusFilter() throws Exception {
        String done = issue(organizationId, projectId, "Done issue", "TASK", "LOW");
        String todo = issue(organizationId, projectId, "Todo issue", "TASK", "LOW");
        assign(organizationId, projectId, done, devProjectMemberId);
        assign(organizationId, projectId, todo, devProjectMemberId);
        changeStatus(done, "IN_PROGRESS");

        myIssues(devToken, "?status=IN_PROGRESS").andExpect(jsonPath("$.content[*].title").value(contains("Done issue")));
        myIssues(devToken, "?status=TODO").andExpect(jsonPath("$.content[*].title").value(contains("Todo issue")));
    }

    @Test
    void leavingOrganizationRemovesItsIssuesFromMyIssues() throws Exception {
        String first = issue(organizationId, projectId, "First org issue", "TASK", "LOW");
        String second = issue(secondOrganizationId, secondProjectId, "Second org issue", "TASK", "LOW");
        assign(organizationId, projectId, first, devProjectMemberId);
        assign(secondOrganizationId, secondProjectId, second, devSecondProjectMemberId);

        leave(devToken, organizationId).andExpect(status().isNoContent());

        myIssues(devToken, "").andExpect(jsonPath("$.content[*].title").value(contains("Second org issue")));
    }

    @Test
    void myIssuesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/users/me/issues")).andExpect(status().isUnauthorized());
    }

    private String createProject(String token, String orgId, String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects", orgId), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private String addToProject(String token, String orgId, String targetProjectId, String memberId) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", orgId, targetProjectId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationMemberId\":\"%s\",\"role\":\"USER\"}".formatted(memberId)))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private String issue(String orgId, String targetProjectId, String title, String type, String priority) throws Exception {
        String extra = type.equals("BUG") ? ",\"severity\":\"MINOR\"" : "";
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", orgId, targetProjectId), ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"%s\",\"title\":\"%s\",\"priority\":\"%s\"%s}".formatted(type, title, priority, extra)))
                .andExpect(status().isCreated()).andReturn(), "$.id");
    }

    private void assign(String orgId, String targetProjectId, String issueId, String projectMemberId) throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}/assignee", orgId, targetProjectId, issueId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectMemberId\":\"%s\"}".formatted(projectMemberId)))
                .andExpect(status().isOk());
    }

    private void changeStatus(String issueId, String status) throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}/status", organizationId, projectId, issueId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"%s\"}".formatted(status)))
                .andExpect(status().isOk());
    }

    private ResultActions list(String query) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/" + organizationId + "/projects/" + projectId + "/issues" + query), ownerToken));
    }

    private ResultActions search(String q) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues", organizationId, projectId), ownerToken)
                .param("q", q));
    }

    private ResultActions myIssues(String token, String query) throws Exception {
        return mockMvc.perform(withToken(get("/users/me/issues" + query), token));
    }
}
