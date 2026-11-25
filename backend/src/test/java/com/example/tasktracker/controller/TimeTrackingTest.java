package com.example.tasktracker.controller;

import com.example.tasktracker.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TimeTrackingTest extends AbstractIntegrationTest {
    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    private String ownerToken;
    private String devToken;
    private String otherDevToken;
    private String strangerToken;
    private String organizationId;
    private String devMemberId;
    private String devProjectMemberId;
    private String projectId;
    private String issueId;

    @BeforeEach
    void setUp() throws Exception {
        ownerToken = registerAndLogin("owner");
        devToken = registerAndLogin("dev");
        otherDevToken = registerAndLogin("otherdev");
        strangerToken = registerAndLogin("stranger");

        organizationId = createOrganization(ownerToken, "Time Org");
        String inviteCode = getInviteCode(ownerToken, organizationId);
        devMemberId = joinAndGetMemberId(devToken, inviteCode);
        String otherDevMemberId = joinAndGetMemberId(otherDevToken, inviteCode);
        joinAndGetMemberId(strangerToken, inviteCode);

        projectId = createProject("Time project");
        devProjectMemberId = addToProject(devMemberId, "USER");
        addToProject(otherDevMemberId, "USER");
        issueId = createIssue("{\"type\":\"TASK\",\"title\":\"Build report\",\"estimateMinutes\":480,\"assigneeId\":\"%s\"}"
                .formatted(devProjectMemberId));
    }

    @Test
    void memberLogsTimeAndIssueShowsSpentAndRemaining() throws Exception {
        logWork(devToken, issueId, 90, TODAY, "Drafted the layout")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.minutes").value(90))
                .andExpect(jsonPath("$.authorUsername").value("dev"))
                .andExpect(jsonPath("$.comment").value("Drafted the layout"))
                .andExpect(jsonPath("$.workDate").value(TODAY.toString()));
        logWork(devToken, issueId, 30, TODAY.minusDays(1), null).andExpect(status().isCreated());

        issue(devToken, issueId)
                .andExpect(jsonPath("$.estimateMinutes").value(480))
                .andExpect(jsonPath("$.spentMinutes").value(120))
                .andExpect(jsonPath("$.remainingMinutes").value(360));

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}/history", organizationId, projectId, issueId), devToken))
                .andExpect(jsonPath("$.content[?(@.type == 'WORK_LOGGED')]", hasSize(2)));
    }

    @Test
    void workLogsAreListedNewestWorkFirst() throws Exception {
        logWork(devToken, issueId, 30, TODAY.minusDays(3), "old").andExpect(status().isCreated());
        logWork(devToken, issueId, 45, TODAY, "new").andExpect(status().isCreated());

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}/worklogs", organizationId, projectId, issueId), ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].comment").value("new"))
                .andExpect(jsonPath("$.content[1].comment").value("old"));
    }

    @Test
    void remainingNeverGoesBelowZero() throws Exception {
        String smallIssue = createIssue("{\"type\":\"TASK\",\"title\":\"Small fix\",\"estimateMinutes\":60}");
        logWork(devToken, smallIssue, 120, TODAY, null).andExpect(status().isCreated());

        issue(devToken, smallIssue)
                .andExpect(jsonPath("$.spentMinutes").value(120))
                .andExpect(jsonPath("$.remainingMinutes").value(0));
    }

    @Test
    void estimateCanBeChangedAndClearedAndIsRecordedInHistory() throws Exception {
        updateIssue(issueId, "{\"estimateMinutes\":600}").andExpect(jsonPath("$.estimateMinutes").value(600));
        updateIssue(issueId, "{\"estimateMinutes\":0}")
                .andExpect(jsonPath("$.estimateMinutes").doesNotExist())
                .andExpect(jsonPath("$.remainingMinutes").doesNotExist());

        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}/history", organizationId, projectId, issueId), ownerToken))
                .andExpect(jsonPath("$.content[?(@.field == 'estimate')]", hasSize(2)));
    }

    @Test
    void epicSumsEstimatesAndTimeOfItsIssues() throws Exception {
        String epicId = createIssue("{\"type\":\"EPIC\",\"title\":\"Reporting\"}");
        String first = createIssue("{\"type\":\"TASK\",\"title\":\"First part\",\"estimateMinutes\":120,\"parentId\":\"%s\"}".formatted(epicId));
        String second = createIssue("{\"type\":\"STORY\",\"title\":\"Second part\",\"estimateMinutes\":180,\"parentId\":\"%s\"}".formatted(epicId));
        logWork(devToken, first, 60, TODAY, null).andExpect(status().isCreated());
        logWork(devToken, second, 30, TODAY, null).andExpect(status().isCreated());

        issue(ownerToken, epicId)
                .andExpect(jsonPath("$.estimateMinutes").value(300))
                .andExpect(jsonPath("$.spentMinutes").value(90))
                .andExpect(jsonPath("$.remainingMinutes").value(210));
    }

    @Test
    void epicRejectsOwnEstimateAndWorkLogs() throws Exception {
        mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EPIC\",\"title\":\"Big epic\",\"estimateMinutes\":60}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("EPIC estimate is calculated from its issues"));

        String epicId = createIssue("{\"type\":\"EPIC\",\"title\":\"Big epic\"}");
        logWork(devToken, epicId, 30, TODAY, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Log time on the issues of an epic, not on the epic itself"));
    }

    @Test
    void organizationAdminMustJoinTheProjectBeforeLoggingTime() throws Exception {
        logWork(ownerToken, issueId, 30, TODAY, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Add yourself to the project before logging time"));
    }

    @Test
    void organizationMemberOutsideTheProjectCannotLogOrReadTime() throws Exception {
        logWork(strangerToken, issueId, 30, TODAY, null).andExpect(status().isForbidden());
        mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}/worklogs", organizationId, projectId, issueId), strangerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidWorkLogs() throws Exception {
        logWork(devToken, issueId, 30, TODAY.plusDays(3), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Work date cannot be in the future"));
        logWork(devToken, issueId, 0, TODAY, null).andExpect(status().isBadRequest());
        logWork(devToken, issueId, 1441, TODAY, null).andExpect(status().isBadRequest());

        logWork(devToken, issueId, 1000, TODAY, null).andExpect(status().isCreated());
        logWork(devToken, issueId, 500, TODAY, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Cannot log more than 24 hours for one day"));
    }

    @Test
    void authorEditsOwnWorkLogButNotSomeoneElses() throws Exception {
        String workLogId = read(logWork(devToken, issueId, 60, TODAY, "first try").andReturn(), "$.id");

        editWorkLog(devToken, workLogId, "{\"minutes\":75,\"comment\":\"\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutes").value(75))
                .andExpect(jsonPath("$.comment").doesNotExist());

        editWorkLog(otherDevToken, workLogId, "{\"minutes\":5}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the author or project ADMIN can change this work log"));
        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/issues/{i}/worklogs/{w}",
                        organizationId, projectId, issueId, workLogId), otherDevToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void organizationOwnerCanRemoveAnyWorkLog() throws Exception {
        String workLogId = read(logWork(devToken, issueId, 60, TODAY, null).andReturn(), "$.id");

        mockMvc.perform(withToken(delete("/organizations/{o}/projects/{p}/issues/{i}/worklogs/{w}",
                        organizationId, projectId, issueId, workLogId), ownerToken))
                .andExpect(status().isNoContent());
        issue(devToken, issueId).andExpect(jsonPath("$.spentMinutes").value(0));
    }

    @Test
    void unknownWorkLogIsNotFound() throws Exception {
        editWorkLog(devToken, "00000000-0000-0000-0000-000000000001", "{\"minutes\":5}").andExpect(status().isNotFound());
    }

    @Test
    void timesheetShowsOnlyMyTimeInThePeriod() throws Exception {
        logWork(devToken, issueId, 60, TODAY, "today").andExpect(status().isCreated());
        logWork(devToken, issueId, 30, TODAY.minusDays(20), "long ago").andExpect(status().isCreated());
        logWork(otherDevToken, issueId, 45, TODAY, "teammate").andExpect(status().isCreated());

        timesheet(devToken, TODAY.minusDays(6), TODAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].minutes").value(60))
                .andExpect(jsonPath("$[0].issueTitle").value("Build report"))
                .andExpect(jsonPath("$[0].projectTitle").value("Time project"))
                .andExpect(jsonPath("$[0].organizationTitle").value("Time Org"));
    }

    @Test
    void timesheetRejectsInvalidPeriods() throws Exception {
        timesheet(devToken, TODAY, TODAY.minusDays(1)).andExpect(status().isBadRequest());
        timesheet(devToken, TODAY.minusDays(100), TODAY).andExpect(status().isBadRequest());
    }

    @Test
    void projectWorkloadShowsTimeAndRemainingWorkPerMember() throws Exception {
        logWork(devToken, issueId, 120, TODAY, null).andExpect(status().isCreated());
        createIssue("{\"type\":\"TASK\",\"title\":\"Huge task\",\"estimateMinutes\":3000,\"assigneeId\":\"%s\"}".formatted(devProjectMemberId));
        createIssue("{\"type\":\"TASK\",\"title\":\"No estimate\",\"assigneeId\":\"%s\"}".formatted(devProjectMemberId));

        projectWorkload(ownerToken, TODAY.minusDays(6), TODAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedMinutes").value(120))
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].username").value("dev"))
                .andExpect(jsonPath("$.members[0].loggedMinutes").value(120))
                .andExpect(jsonPath("$.members[0].openIssues").value(3))
                .andExpect(jsonPath("$.members[0].unestimatedIssues").value(1))
                .andExpect(jsonPath("$.members[0].remainingEstimateMinutes").value(3360))
                .andExpect(jsonPath("$.members[0].overloaded").value(true))
                .andExpect(jsonPath("$.members[0].days", hasSize(7)))
                .andExpect(jsonPath("$.members[0].days[6].minutes").value(120))
                .andExpect(jsonPath("$.members[0].projects[0].projectTitle").value("Time project"))
                .andExpect(jsonPath("$.members[1].username").value("otherdev"))
                .andExpect(jsonPath("$.members[1].overloaded").value(false));
    }

    @Test
    void projectWorkloadIsForAdminsOnly() throws Exception {
        projectWorkload(devToken, TODAY.minusDays(6), TODAY).andExpect(status().isForbidden());
        projectWorkload(strangerToken, TODAY.minusDays(6), TODAY).andExpect(status().isForbidden());
    }

    @Test
    void organizationWorkloadAndCapacityAreForOwnersAndAdmins() throws Exception {
        logWork(devToken, issueId, 60, TODAY, null).andExpect(status().isCreated());

        mockMvc.perform(withToken(patch("/organizations/{o}/members/{m}/capacity", organizationId, devMemberId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weeklyCapacityMinutes\":1200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklyCapacityMinutes").value(1200));

        mockMvc.perform(withToken(get("/organizations/{o}/workload", organizationId), ownerToken)
                        .param("from", TODAY.minusDays(6).toString())
                        .param("to", TODAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members", hasSize(4)))
                .andExpect(jsonPath("$.members[0].username").value("dev"))
                .andExpect(jsonPath("$.members[0].weeklyCapacityMinutes").value(1200))
                .andExpect(jsonPath("$.members[0].capacityMinutes").value(1200))
                .andExpect(jsonPath("$.workingDays").value(5));

        mockMvc.perform(withToken(get("/organizations/{o}/workload", organizationId), devToken)
                        .param("from", TODAY.minusDays(6).toString())
                        .param("to", TODAY.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(patch("/organizations/{o}/members/{m}/capacity", organizationId, devMemberId), devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weeklyCapacityMinutes\":3000}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(withToken(patch("/organizations/{o}/members/{m}/capacity", organizationId, devMemberId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weeklyCapacityMinutes\":20000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timesheetHidesTimeFromOrganizationsILeft() throws Exception {
        logWork(devToken, issueId, 60, TODAY, null).andExpect(status().isCreated());
        leave(devToken, organizationId).andExpect(status().isNoContent());

        timesheet(devToken, TODAY.minusDays(6), TODAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void organizationWorkloadIgnoresOpenIssuesOfArchivedProjects() throws Exception {
        mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/status", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(withToken(get("/organizations/{o}/workload", organizationId), ownerToken)
                        .param("from", TODAY.minusDays(6).toString())
                        .param("to", TODAY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingEstimateMinutes").value(0))
                .andExpect(jsonPath("$.members[?(@.username == 'dev')].openIssues").value(0));
    }

    private ResultActions logWork(String token, String targetIssueId, int minutes, LocalDate date, String comment) throws Exception {
        String body = comment == null
                ? "{\"minutes\":%d,\"workDate\":\"%s\"}".formatted(minutes, date)
                : "{\"minutes\":%d,\"workDate\":\"%s\",\"comment\":\"%s\"}".formatted(minutes, date, comment);
        return mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues/{i}/worklogs", organizationId, projectId, targetIssueId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions editWorkLog(String token, String workLogId, String body) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}/worklogs/{w}",
                        organizationId, projectId, issueId, workLogId), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions issue(String token, String targetIssueId) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/issues/{i}", organizationId, projectId, targetIssueId), token))
                .andExpect(status().isOk());
    }

    private ResultActions updateIssue(String targetIssueId, String body) throws Exception {
        return mockMvc.perform(withToken(patch("/organizations/{o}/projects/{p}/issues/{i}", organizationId, projectId, targetIssueId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private ResultActions timesheet(String token, LocalDate from, LocalDate to) throws Exception {
        return mockMvc.perform(withToken(get("/users/me/worklogs"), token)
                .param("from", from.toString())
                .param("to", to.toString()));
    }

    private ResultActions projectWorkload(String token, LocalDate from, LocalDate to) throws Exception {
        return mockMvc.perform(withToken(get("/organizations/{o}/projects/{p}/workload", organizationId, projectId), token)
                .param("from", from.toString())
                .param("to", to.toString()));
    }

    private String createProject(String title) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{id}/projects", organizationId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"%s\"}".formatted(title)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private String addToProject(String organizationMemberId, String role) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/members", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"organizationMemberId\":\"%s\",\"role\":\"%s\"}".formatted(organizationMemberId, role)))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }

    private String createIssue(String body) throws Exception {
        return read(mockMvc.perform(withToken(post("/organizations/{o}/projects/{p}/issues", organizationId, projectId), ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn(), "$.id");
    }
}
