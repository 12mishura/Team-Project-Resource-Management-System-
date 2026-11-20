package com.example.tasktracker.service;

import com.example.tasktracker.domain.Bug;
import com.example.tasktracker.domain.IssueEvent;
import com.example.tasktracker.domain.Story;
import com.example.tasktracker.domain.Task;
import com.example.tasktracker.domain.User;
import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssueEventType;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.repository.IssueEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class IssueHistoryRecorderTest {
    private final IssueEventRepository repository = mock(IssueEventRepository.class);
    private final IssueHistoryRecorder recorder = new IssueHistoryRecorder(repository);
    private final User actor = new User("alice@mail.com", "alice", "hash");

    @Test
    void snapshotContainsOnlyFieldsOfIssueType() {
        assertThat(recorder.snapshot(new Task("Task", null, IssuePriority.LOW)).keySet())
                .containsExactly("title", "description", "priority");
        assertThat(recorder.snapshot(new Story("Story", null, null, 5)))
                .containsEntry("storyPoints", "5").doesNotContainKey("severity");
        assertThat(recorder.snapshot(new Bug("Bug", null, null, BugSeverity.MAJOR, "Steps")))
                .containsEntry("severity", "MAJOR").containsEntry("stepsToReproduce", "Steps").doesNotContainKey("storyPoints");
    }

    @Test
    void unchangedValuesAreNotRecorded() {
        Task task = new Task("Task", null, null);

        recorder.recordChange(task, actor, IssueEventType.STATUS_CHANGED, "status", "TODO", "TODO");
        recorder.recordChange(task, actor, IssueEventType.ASSIGNEE_CHANGED, "assignee", null, null);

        verify(repository, never()).save(any());
    }

    @Test
    void fieldChangesProduceOneEventPerChangedField() {
        Task task = new Task("Task", null, null);
        Map<String, String> before = recorder.snapshot(task);
        task.setTitle("Renamed");
        task.setPriority(IssuePriority.HIGH);

        recorder.recordFieldChanges(task, actor, before, recorder.snapshot(task));

        ArgumentCaptor<IssueEvent> events = ArgumentCaptor.forClass(IssueEvent.class);
        verify(repository, times(2)).save(events.capture());
        assertThat(events.getAllValues()).extracting(IssueEvent::getField).containsExactly("title", "priority");
        assertThat(events.getAllValues().getFirst().getOldValue()).isEqualTo("Task");
        assertThat(events.getAllValues().getFirst().getNewValue()).isEqualTo("Renamed");
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.getActor()).isSameAs(actor);
            assertThat(event.getIssue()).isSameAs(task);
        });
    }
}
