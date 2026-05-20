package com.koushik.course_catalog.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {
    @NotBlank(message = "ID must not be blank")
    private Long id;

    @NotBlank(message = "Name must not be blank")
    private String name;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email must not be blank")
    private String email;

    @NotNull(message = "Role is required")
    @NotBlank(message = "Role must not be blank")
    private String role;

    @NotBlank(message = "Department must not be blank")
    private String department;

    @NotNull(message = "Salary is required")
    @Positive(message = "Salary must be greater than zero")
    private Double salary;

    @NotNull(message = "Experience is required")
    @Positive(message = "Experience must be greater than zero")
    private Double experience;
}
