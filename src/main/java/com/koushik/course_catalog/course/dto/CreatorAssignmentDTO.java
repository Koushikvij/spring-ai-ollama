package com.koushik.course_catalog.course.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorAssignmentDTO {

    @NotEmpty(message = "Content creator IDs are required")
    private List<Long> contentCreatorIds = new ArrayList<>();
}
