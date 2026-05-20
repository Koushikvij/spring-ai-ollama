package com.koushik.course_catalog.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.koushik.course_catalog.common.exception.BadRequestException;
import com.koushik.course_catalog.common.saga.CatalogSagaService;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.employee.dto.EmployeeRequestDTO;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CatalogSagaService catalogSagaService;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(
                employeeRepository, courseRepository, catalogSagaService, new ModelMapper());
    }

    @Test
    void addEmployee_savesSuccessfully() {
        EmployeeRequestDTO request = new EmployeeRequestDTO(
                "Bob", "bob@test.com", "Instructor", "Eng", 80_000d, 5d);
        Employee saved = new Employee(1L, "Bob", "bob@test.com", "Instructor", "Eng", 80_000d, 5d);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        assertThat(employeeService.addEmployee(request)).contains("Bob");
    }

    @Test
    void addEmployee_throwsWhenIdMissing() {
        EmployeeRequestDTO request = new EmployeeRequestDTO(
                "Bob", "bob@test.com", "Instructor", "Eng", 80_000d, 5d);
        Employee saved = new Employee(null, "Bob", "bob@test.com", "Instructor", "Eng", 80_000d, 5d);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        assertThatThrownBy(() -> employeeService.addEmployee(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCoursesByEmployeeId_returnsMappedCourses() {
        when(employeeRepository.findById(2L))
                .thenReturn(Optional.of(new Employee(2L, "Bob", "b@test.com", "R", "D", 1d, 1d)));
        Course course = new Course(10L, "Java", "Desc", List.of(2L), List.of());
        when(courseRepository.findByContentCreatorId(2L)).thenReturn(List.of(course));

        List<CourseResponseDTO> courses = employeeService.getCoursesByEmployeeId(2L);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getTitle()).isEqualTo("Java");
    }

    @Test
    void deleteEmployee_delegatesToSaga() {
        when(catalogSagaService.deleteEmployee(2L)).thenReturn("Employee deleted successfully");

        assertThat(employeeService.deleteEmployee(2L)).contains("deleted");
        verify(catalogSagaService).deleteEmployee(2L);
    }
}
