package com.koushik.course_catalog.course.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.koushik.course_catalog.common.exception.ResourceNotFoundException;
import com.koushik.course_catalog.common.saga.CatalogSagaService;
import com.koushik.course_catalog.course.dto.CourseRequestDTO;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private CatalogSagaService catalogSagaService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                customerRepository,
                employeeRepository,
                catalogSagaService,
                new ModelMapper());
    }

    @Test
    void addCourse_delegatesToSaga() {
        CourseRequestDTO request = new CourseRequestDTO("T", "D", List.of(1L), List.of());
        when(catalogSagaService.createCourse(request)).thenReturn("course-1");

        assertThat(courseService.addCourse(request)).isEqualTo("course-1");
        verify(catalogSagaService).createCourse(request);
    }

    @Test
    void getCourse_returnsMappedDto() {
        Course course = new Course(1L, "Java", "Desc", List.of(2L), List.of(3L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        CourseResponseDTO dto = courseService.getCourse(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Java");
    }

    @Test
    void getCourse_throwsWhenNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.getCourse(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllCourses_returnsMappedPage() {
        Course course = new Course(1L, "Java", "Desc", List.of(), List.of());
        Page<Course> page = new PageImpl<>(List.of(course));
        when(courseRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<CourseResponseDTO> result = courseService.getAllCourses(PageRequest.of(0, 5));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Java");
    }

    @Test
    void getCustomersByCourseId_returnsEmptyWhenNoEnrollments() {
        Course course = new Course(1L, "Java", "Desc", List.of(), List.of());
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThat(courseService.getCustomersByCourseId(1L)).isEmpty();
    }

    @Test
    void getCustomersByCourseId_returnsCustomers() {
        Course course = new Course(1L, "Java", "Desc", List.of(), List.of(10L));
        Customer customer = new Customer(10L, "Ann", "a@test.com", "+1", List.of(1L));
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(customerRepository.findAllById(List.of(10L))).thenReturn(List.of(customer));

        assertThat(courseService.getCustomersByCourseId(1L))
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.getEmail()).isEqualTo("a@test.com"));
    }

    @Test
    void getCreatorsByCourseId_returnsEmployees() {
        Course course = new Course(1L, "Java", "Desc", List.of(5L), List.of());
        Employee employee = new Employee(5L, "Bob", "b@test.com", "Instructor", "Eng", 90_000d, 3d);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(employeeRepository.findAllById(List.of(5L))).thenReturn(List.of(employee));

        assertThat(courseService.getCreatorsByCourseId(1L))
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.getName()).isEqualTo("Bob"));
    }

    @Test
    void enrollCustomer_delegatesToSaga() {
        when(catalogSagaService.enrollCustomer(1L, 10L)).thenReturn("ok");
        assertThat(courseService.enrollCustomer(1L, 10L)).isEqualTo("ok");
        verify(catalogSagaService).enrollCustomer(1L, 10L);
    }
}
