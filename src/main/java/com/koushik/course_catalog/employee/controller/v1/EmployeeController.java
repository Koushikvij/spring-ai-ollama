package com.koushik.course_catalog.employee.controller.v1;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.koushik.course_catalog.common.entity.PageSettings;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.employee.dto.EmployeeRequestDTO;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;
import com.koushik.course_catalog.employee.service.EmployeeService;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee APIs", description = "CRUD + Pagination APIs for Employees")
public class EmployeeController {

    private static final Logger log =
            LoggerFactory.getLogger(EmployeeController.class);
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService, ModelMapper modelMapper) {
        this.employeeService = employeeService;
    }

    @PostMapping("addEmployee")
    @Operation(summary = "Add a new employee", description = "Creates a new employee record in the system")
    public ResponseEntity<String> addEmployee(@Valid @RequestBody EmployeeRequestDTO requestDTO) {
        log.info("Creating new employee with email={}", requestDTO.getEmail());
        String employee = employeeService.addEmployee(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("Employee added successfully " + employee);
    }
    
    @PutMapping("updateEmployee/{id}")
    @Operation(summary = "Update an existing employee", description = "Updates the details of an existing employee by ID")
    public ResponseEntity<String> updateEmployee(@PathVariable Long id, @Valid @RequestBody EmployeeRequestDTO requestDTO) {
        log.info("Updating employee with ID={}", id);
        String response = employeeService.updateEmployee(id, requestDTO);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Employee updated successfully " + response);
    }
    
    @DeleteMapping("deleteEmployee/{id}")
    @Operation(summary = "Delete an employee", description = "Deletes an employee record from the system by ID")
    public ResponseEntity<String> deleteEmployee(@PathVariable Long id) {
        log.info("Deleting employee with ID={}", id);
        String response = employeeService.deleteEmployee(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Employee deleted successfully " + response);
    }

    @GetMapping("getEmployee/{id}")
    @Operation(summary = "Get employee details", description = "Retrieves the details of an employee by ID")
    public ResponseEntity<String> getEmployee(@PathVariable Long id) {
        log.info("Retrieving employee with ID={}", id);
        EmployeeResponseDTO response = employeeService.getEmployee(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body("Employee retrieved successfully: " + response);
    }
    
    @GetMapping("getAllEmployees")
    @Operation(summary = "Get all employees with pagination", description = "Retrieves a paginated list of all employees, with optional sorting")
    public ResponseEntity<Page<EmployeeResponseDTO>> getAllEmployees(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        log.info("Retrieving all employees");
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        int size = PageSettings.getEmployeePageSize();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EmployeeResponseDTO> employeePage = employeeService.getAllEmployees(pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(employeePage);
    }
    
    @PostMapping("setRecordsinPage")
    @Operation(summary = "Set the number of records to display per page", description = "Configures the number of employee records to show on each page of the results")
    public void postMethodName(@RequestParam int size) {
        log.info("Setting records in page to {}", size);
        employeeService.setRecordsInPage(size);
    }

    @GetMapping("getCoursesByEmployee/{employeeId}")
    @Operation(summary = "Get courses by employee", description = "Retrieves course details for all courses where the given employee is a content creator")
    public ResponseEntity<List<CourseResponseDTO>> getCoursesByEmployee(@PathVariable Long employeeId) {
        log.info("Retrieving courses for employee ID={}", employeeId);
        List<CourseResponseDTO> courses = employeeService.getCoursesByEmployeeId(employeeId);
        return ResponseEntity.ok(courses);
    }
}
