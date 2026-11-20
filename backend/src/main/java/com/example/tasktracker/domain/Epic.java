package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("EPIC")
@NoArgsConstructor
public class Epic extends Issue {

    public Epic(String title, String description, IssuePriority priority){
        super(title, description, priority);
    }

    @Override
    public IssueType getType(){
        return IssueType.EPIC;
    }

    @Override
    public boolean canHaveParent(){
        return false;
    }
}
