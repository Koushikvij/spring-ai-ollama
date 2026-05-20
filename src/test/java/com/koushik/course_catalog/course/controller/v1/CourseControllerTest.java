package com.koushik.course_catalog.course.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.koushik.course_catalog.course.service.CourseService;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        TestPageSettings.resetToDefault();
        CourseController controller = new CourseController(courseService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllCourses_returnsPage() throws Exception {
        CourseResponseDTO dto = new CourseResponseDTO(1L, "Java", "Desc", List.of(1L), List.of(2L));
        when(courseService.getAllCourses(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/v1/courses/getAllCourses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Java"));
    }

    @Test
    void enrollCustomer_returnsOk() throws Exception {
        when(courseService.enrollCustomer(1L, 10L))
                .thenReturn("Customer 10 enrolled in course 1");

        mockMvc.perform(post("/api/v1/courses/enrollCustomer/1/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Customer 10 enrolled in course 1"));
    }

    @Test
    void getCustomersByCourse_returnsList() throws Exception {
        CustomerResponseDTO customer = new CustomerResponseDTO(10L, "Ann", "a@test.com", "+1", List.of(1L));
        when(courseService.getCustomersByCourseId(1L)).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/v1/courses/getCustomersByCourse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@test.com"));
    }

    @Test
    void assignCreators_returnsOk() throws Exception {
        when(courseService.assignCreators(eq(1L), any())).thenReturn("Creators assigned to course 1");

        mockMvc.perform(put("/api/v1/courses/assignCreators/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentCreatorIds":[1,2]}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void addCourse_returnsCreated() throws Exception {
        when(courseService.addCourse(any())).thenReturn("Course[id=1]");

        mockMvc.perform(post("/api/v1/courses/addCourse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Spring Boot",
                                  "description": "Intro",
                                  "contentCreatorIds": [1],
                                  "enrolledCustomerIds": []
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void getCreatorsByCourse_returnsList() throws Exception {
        EmployeeResponseDTO employee = new EmployeeResponseDTO(
                1L, "Bob", "b@test.com", "Instructor", "Eng", 90_000d, 5d);
        when(courseService.getCreatorsByCourseId(1L)).thenReturn(List.of(employee));

        mockMvc.perform(get("/api/v1/courses/getCreatorsByCourse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bob"));
    }

    @Test
    void unenrollCustomer_returnsOk() throws Exception {
        when(courseService.unenrollCustomer(1L, 10L))
                .thenReturn("Customer 10 unenrolled from course 1");

        mockMvc.perform(delete("/api/v1/courses/unenrollCustomer/1/10"))
                .andExpect(status().isOk());
    }
}
