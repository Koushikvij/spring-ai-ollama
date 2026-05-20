package com.koushik.course_catalog.course.controller.v1;

import com.koushik.course_catalog.common.entity.PageSettings;
import com.koushik.course_catalog.course.dto.CourseRequestDTO;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.course.dto.CreatorAssignmentDTO;
import com.koushik.course_catalog.course.service.CourseService;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Course APIs", description = "CRUD + Pagination APIs for Courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);
    private final CourseService courseService;

    public CourseController(CourseService courseService, ModelMapper modelMapper) {
        this.courseService = courseService;
    }

    @PostMapping("addCourse")
    @Operation(summary = "Add a new course", description = "Creates a new course record in the system")
    public ResponseEntity<String> addCourse(@Valid @RequestBody CourseRequestDTO requestDTO) {
        log.info("Creating new course with title={}", requestDTO.getTitle());
        String course = courseService.addCourse(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Course added successfully " + course);
    }

    @PutMapping("updateCourse/{id}")
    @Operation(summary = "Update an existing course", description = "Updates the details of an existing course by ID")
    public ResponseEntity<String> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseRequestDTO requestDTO) {
        log.info("Updating course with ID={}", id);
        String response = courseService.updateCourse(id, requestDTO);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Course updated successfully " + response);
    }

    @DeleteMapping("deleteCourse/{id}")
    @Operation(summary = "Delete a course", description = "Deletes a course record from the system by ID")
    public ResponseEntity<String> deleteCourse(@PathVariable Long id) {
        log.info("Deleting course with ID={}", id);
        String response = courseService.deleteCourse(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Course deleted successfully " + response);
    }

    @GetMapping("getCourse/{id}")
    @Operation(summary = "Get course details", description = "Retrieves the details of a course by ID")
    public ResponseEntity<String> getCourse(@PathVariable Long id) {
        log.info("Retrieving course with ID={}", id);
        CourseResponseDTO response = courseService.getCourse(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Course retrieved successfully: " + response);
    }

    @GetMapping("getAllCourses")
    @Operation(summary = "Get all courses with pagination", description = "Retrieves a paginated list of all courses, with optional sorting")
    public ResponseEntity<Page<CourseResponseDTO>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        log.info("Retrieving all courses");
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        int size = PageSettings.getCoursePageSize();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<CourseResponseDTO> coursePage = courseService.getAllCourses(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(coursePage);
    }

    @PostMapping("setRecordsinPage")
    @Operation(summary = "Set the number of records to display per page", description = "Configures the number of course records to show on each page of the results")
    public void postMethodName(@RequestParam int size) {
        log.info("Setting records in page to {}", size);
        courseService.setRecordsInPage(size);
    }

    @GetMapping("getCustomersByCourse/{courseId}")
    @Operation(summary = "Get enrolled customers for a course", description = "Retrieves customer details for all customers enrolled in the given course")
    public ResponseEntity<List<CustomerResponseDTO>> getCustomersByCourse(@PathVariable Long courseId) {
        log.info("Retrieving customers for course ID={}", courseId);
        List<CustomerResponseDTO> customers = courseService.getCustomersByCourseId(courseId);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("getCreatorsByCourse/{courseId}")
    @Operation(summary = "Get content creators for a course", description = "Retrieves employee details for all content creators assigned to the given course")
    public ResponseEntity<List<EmployeeResponseDTO>> getCreatorsByCourse(@PathVariable Long courseId) {
        log.info("Retrieving creators for course ID={}", courseId);
        List<EmployeeResponseDTO> creators = courseService.getCreatorsByCourseId(courseId);
        return ResponseEntity.ok(creators);
    }

    @PostMapping("enrollCustomer/{courseId}/{customerId}")
    @Operation(summary = "Enroll customer in course (saga)", description = "Atomically links customer and course enrollment across both databases with compensation on failure")
    public ResponseEntity<String> enrollCustomer(@PathVariable Long courseId, @PathVariable Long customerId) {
        log.info("Enrolling customer {} in course {} via saga", customerId, courseId);
        String result = courseService.enrollCustomer(courseId, customerId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("unenrollCustomer/{courseId}/{customerId}")
    @Operation(summary = "Unenroll customer from course (saga)", description = "Atomically removes enrollment links from course and customer databases")
    public ResponseEntity<String> unenrollCustomer(@PathVariable Long courseId, @PathVariable Long customerId) {
        log.info("Unenrolling customer {} from course {} via saga", customerId, courseId);
        String result = courseService.unenrollCustomer(courseId, customerId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("assignCreators/{courseId}")
    @Operation(summary = "Assign content creators (saga)", description = "Validates employees exist and updates course creator IDs with rollback on failure")
    public ResponseEntity<String> assignCreators(
            @PathVariable Long courseId,
            @Valid @RequestBody CreatorAssignmentDTO request) {
        log.info("Assigning creators to course {} via saga", courseId);
        String result = courseService.assignCreators(courseId, request.getContentCreatorIds());
        return ResponseEntity.ok(result);
    }
}

