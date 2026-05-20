package com.koushik.course_catalog.common.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.koushik.course_catalog.common.exception.BadRequestException;
import com.koushik.course_catalog.common.exception.ResourceNotFoundException;
import com.koushik.course_catalog.course.dto.CourseRequestDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class CatalogSagaServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private CatalogSagaService catalogSagaService;

    @BeforeEach
    void setUp() {
        catalogSagaService = new CatalogSagaService(
                new SagaOrchestrator(),
                courseRepository,
                customerRepository,
                employeeRepository,
                new ModelMapper());
    }

    @Test
    void enrollCustomer_updatesCourseAndCustomer() {
        Course course = course(1L, List.of(), List.of());
        Customer customer = customer(10L, List.of());

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = catalogSagaService.enrollCustomer(1L, 10L);

        assertThat(result).contains("enrolled");
        assertThat(course.getEnrolledCustomerIds()).contains(10L);
        assertThat(customer.getEnrolledCourseIds()).contains(1L);
    }

    @Test
    void enrollCustomer_throwsWhenCourseMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogSagaService.enrollCustomer(99L, 10L))
                .isInstanceOf(SagaException.class)
                .hasCauseInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCourse_throwsWhenCreatorIdsInvalid() {
        CourseRequestDTO request = new CourseRequestDTO(
                "Title", "Description", List.of(1L, 2L), List.of());

        when(employeeRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(employee(1L)));

        assertThatThrownBy(() -> catalogSagaService.createCourse(request))
                .isInstanceOf(SagaException.class)
                .hasCauseInstanceOf(BadRequestException.class);

        verify(courseRepository, never()).save(any());
    }

    @Test
    void createCourse_persistsCourseAndLinksCustomers() {
        CourseRequestDTO request = new CourseRequestDTO(
                "Spring Boot", "Description", List.of(1L), List.of(10L));

        when(employeeRepository.findAllById(List.of(1L))).thenReturn(List.of(employee(1L)));
        when(customerRepository.findAllById(List.of(10L))).thenReturn(List.of(customer(10L, List.of())));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            course.setId(5L);
            return course;
        });

        Course courseAfterEnroll = course(5L, List.of(1L), new ArrayList<>());
        Customer customer = customer(10L, new ArrayList<>());

        when(courseRepository.findById(5L)).thenReturn(Optional.of(courseAfterEnroll));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(courseRepository.save(courseAfterEnroll)).thenReturn(courseAfterEnroll);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = catalogSagaService.createCourse(request);

        assertThat(result).contains("Spring Boot");
        verify(courseRepository, atLeastOnce()).save(any(Course.class));
    }

    @Test
    void deleteCourse_unlinksCustomersAndDeletes() {
        Course course = course(1L, List.of(1L), List.of(10L, 11L));
        Customer c10 = customer(10L, List.of(1L));
        Customer c11 = customer(11L, List.of(1L));

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(c10));
        when(customerRepository.findById(11L)).thenReturn(Optional.of(c11));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        String result = catalogSagaService.deleteCourse(1L);

        assertThat(result).isEqualTo("Course deleted successfully");
        verify(courseRepository).deleteById(1L);
        assertThat(c10.getEnrolledCourseIds()).doesNotContain(1L);
        assertThat(c11.getEnrolledCourseIds()).doesNotContain(1L);
    }

    @Test
    void assignCreators_updatesContentCreatorIds() {
        Course course = course(2L, List.of(99L), List.of());
        when(employeeRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(employee(1L), employee(2L)));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        catalogSagaService.assignCreators(2L, List.of(1L, 2L));

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertThat(captor.getValue().getContentCreatorIds()).containsExactly(1L, 2L);
    }

    private static Course course(Long id, List<Long> creators, List<Long> customers) {
        Course course = new Course();
        course.setId(id);
        course.setTitle("Test Course");
        course.setDescription("Test");
        course.setContentCreatorIds(new ArrayList<>(creators));
        course.setEnrolledCustomerIds(new ArrayList<>(customers));
        return course;
    }

    private static Customer customer(Long id, List<Long> courses) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setName("Customer " + id);
        customer.setEmail("c" + id + "@test.com");
        customer.setPhone("+1-555-0000");
        customer.setEnrolledCourseIds(new ArrayList<>(courses));
        return customer;
    }

    private static Employee employee(Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setName("Employee " + id);
        employee.setEmail("e" + id + "@test.com");
        employee.setRole("Instructor");
        employee.setDepartment("Engineering");
        employee.setSalary(80_000d);
        employee.setExperience(5d);
        return employee;
    }
}
