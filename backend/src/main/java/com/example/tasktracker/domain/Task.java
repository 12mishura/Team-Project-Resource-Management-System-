package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("TASK")
@NoArgsConstructor
public class Task extends Issue {

    public Task(String title, String description, IssuePriority priority){
        super(title, description, priority);
    }

    @Override
    public IssueType getType(){
        return IssueType.TASK;
    }
}
