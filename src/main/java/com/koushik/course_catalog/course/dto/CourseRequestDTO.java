package com.koushik.course_catalog.course.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequestDTO {
    @NotBlank(message = "Course title is required")
    private String title;
    @NotBlank(message = "Course description is required")
    private String description;
    @NotEmpty(message = "Content creator IDs are required")
    private List<Long> contentCreatorIds = new ArrayList<>();
    private List<Long> enrolledCustomerIds = new ArrayList<>();
}
