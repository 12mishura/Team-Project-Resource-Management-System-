package com.example.tasktracker.domain;

import com.example.tasktracker.domain.enums.IssuePriority;
import com.example.tasktracker.domain.enums.IssueType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("STORY")
@Getter @Setter
@NoArgsConstructor
public class Story extends Issue {
    @Column(name = "story_points")
    private Integer storyPoints;

    public Story(String title, String description, IssuePriority priority, Integer storyPoints){
        super(title, description, priority);
        this.storyPoints = storyPoints;
    }

    @Override
    public IssueType getType(){
        return IssueType.STORY;
    }
}
