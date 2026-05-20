package com.koushik.course_catalog.employee.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.koushik.course_catalog.common.exception.GlobalExceptionHandler;
import com.koushik.course_catalog.support.TestPageSettings;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;
import com.koushik.course_catalog.employee.service.EmployeeService;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        TestPageSettings.resetToDefault();
        EmployeeController controller = new EmployeeController(employeeService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllEmployees_returnsPage() throws Exception {
        EmployeeResponseDTO dto = new EmployeeResponseDTO(
                1L, "Bob", "b@test.com", "Instructor", "Eng", 80_000d, 5d);
        when(employeeService.getAllEmployees(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/v1/employees/getAllEmployees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("Instructor"));
    }

    @Test
    void getCoursesByEmployee_returnsList() throws Exception {
        CourseResponseDTO course = new CourseResponseDTO(5L, "Java", "Desc", List.of(1L), List.of());
        when(employeeService.getCoursesByEmployeeId(1L)).thenReturn(List.of(course));

        mockMvc.perform(get("/api/v1/employees/getCoursesByEmployee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java"));
    }

    @Test
    void addEmployee_returnsCreated() throws Exception {
        when(employeeService.addEmployee(any())).thenReturn("Employee[id=1]");

        mockMvc.perform(post("/api/v1/employees/addEmployee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bob",
                                  "email": "bob@test.com",
                                  "role": "Instructor",
                                  "department": "Engineering",
                                  "salary": 80000,
                                  "experience": 5
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
