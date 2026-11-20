package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.BugSeverity;
import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("BUG")
@Getter @Setter
@NoArgsConstructor
public class Bug extends Issue {
    @Enumerated(value = EnumType.STRING)
    @Column(name = "severity")
    private BugSeverity severity;

    @Column(name = "steps_to_reproduce")
    private String stepsToReproduce;

    public Bug(String title, String description, IssuePriority priority, BugSeverity severity, String stepsToReproduce){
        super(title, description, priority);
        this.severity = severity;
        this.stepsToReproduce = stepsToReproduce;
    }

    @Override
    public IssueType getType(){
        return IssueType.BUG;
    }
}
