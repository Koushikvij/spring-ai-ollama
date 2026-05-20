package com.koushik.course_catalog.course.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDTO {
    @NotBlank(message = "Course ID is required")
    private Long id;
    @NotBlank(message = "Course title is required")
    private String title;
    @NotBlank(message = "Course description is required")
    private String description;
    @NotBlank(message = "Content creator IDs are required")
    private List<Long> contentCreatorIds = new ArrayList<>();
    @NotBlank(message = "Enrolled customer IDs are required")
    private List<Long> enrolledCustomerIds = new ArrayList<>();
}
