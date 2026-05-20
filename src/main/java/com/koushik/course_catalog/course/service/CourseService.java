package com.koushik.course_catalog.course.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.koushik.course_catalog.common.entity.PageSettings;
import com.koushik.course_catalog.common.exception.ResourceNotFoundException;
import com.koushik.course_catalog.common.saga.CatalogSagaService;
import com.koushik.course_catalog.course.dto.CourseRequestDTO;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@Service
public class CourseService {
    private final CourseRepository courseRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final CatalogSagaService catalogSagaService;
    private final ModelMapper modelMapper;
    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    public CourseService(
            CourseRepository courseRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            CatalogSagaService catalogSagaService,
            ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.catalogSagaService = catalogSagaService;
        this.modelMapper = modelMapper;
    }

    public String addCourse(CourseRequestDTO requestDTO) {
        log.debug("Creating course via saga");
        return catalogSagaService.createCourse(requestDTO);
    }

    public String updateCourse(Long id, CourseRequestDTO requestDTO) {
        log.debug("Updating course via saga, id={}", id);
        return catalogSagaService.updateCourse(id, requestDTO);
    }

    public String deleteCourse(Long id) {
        log.debug("Deleting course via saga, id={}", id);
        return catalogSagaService.deleteCourse(id);
    }

    public String enrollCustomer(Long courseId, Long customerId) {
        log.debug("Enrolling customer {} in course {} via saga", customerId, courseId);
        return catalogSagaService.enrollCustomer(courseId, customerId);
    }

    public String unenrollCustomer(Long courseId, Long customerId) {
        log.debug("Unenrolling customer {} from course {} via saga", customerId, courseId);
        return catalogSagaService.unenrollCustomer(courseId, customerId);
    }

    public String assignCreators(Long courseId, List<Long> creatorIds) {
        log.debug("Assigning creators to course {} via saga", courseId);
        return catalogSagaService.assignCreators(courseId, creatorIds);
    }

    public CourseResponseDTO getCourse(Long id) {
        log.debug("Retrieving course with ID={}", id);
        Course existingCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        if (Objects.isNull(existingCourse)) {
            throw new ResourceNotFoundException("Course not found with id: " + id);
        }
        log.info("Course retrieved with id={}", existingCourse.getId());
        return modelMapper.map(existingCourse, CourseResponseDTO.class);
    }

    public Page<CourseResponseDTO> getAllCourses(Pageable pageable) {
        log.debug("Retrieving all courses with pagination");
        Page<Course> coursePage = courseRepository.findAll(pageable);
        log.info("Retrieved {} courses", coursePage.getNumberOfElements());
        return coursePage.map(course -> modelMapper.map(course, CourseResponseDTO.class));
    }

    public void setRecordsInPage(int size) {
        log.debug("Setting records in page to {}", size);
        PageSettings.setCoursePageSize(size);
    }

    public List<CustomerResponseDTO> getCustomersByCourseId(Long courseId) {
        log.debug("Retrieving enrolled customers for course ID={}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        List<Long> customerIds = course.getEnrolledCustomerIds();
        if (customerIds == null || customerIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Customer> customers = customerRepository.findAllById(customerIds);
        log.info("Retrieved {} customers for course id={}", customers.size(), courseId);
        return customers.stream()
                .map(customer -> modelMapper.map(customer, CustomerResponseDTO.class))
                .toList();
    }

    public List<EmployeeResponseDTO> getCreatorsByCourseId(Long courseId) {
        log.debug("Retrieving content creators for course ID={}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        List<Long> creatorIds = course.getContentCreatorIds();
        if (creatorIds == null || creatorIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Employee> creators = employeeRepository.findAllById(creatorIds);
        log.info("Retrieved {} creators for course id={}", creators.size(), courseId);
        return creators.stream()
                .map(employee -> modelMapper.map(employee, EmployeeResponseDTO.class))
                .toList();
    }
}
