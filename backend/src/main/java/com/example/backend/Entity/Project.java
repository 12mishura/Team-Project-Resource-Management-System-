package com.example.backend.Entity;

import com.example.backend.Entity.Enum.ProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(length = 100 , nullable = false)
    private String name ;
    private String description;

    //Project Status

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus projectStatus ;

    //Date/DateTime belongings

    private LocalDate startDate;
    private LocalDate endDate;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime created_at;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updated_at;

    //Data Base Relations

    @ManyToOne(optional = false)
    @JoinColumn(name = "organization_id" , nullable = false)
    private Organization organization;
}
