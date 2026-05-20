package com.koushik.course_catalog.employee.service;

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
import com.koushik.course_catalog.common.exception.BadRequestException;
import com.koushik.course_catalog.common.saga.CatalogSagaService;
import com.koushik.course_catalog.course.dto.CourseResponseDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.employee.dto.EmployeeRequestDTO;
import com.koushik.course_catalog.employee.dto.EmployeeResponseDTO;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;


@Service  
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final CourseRepository courseRepository;
    private final CatalogSagaService catalogSagaService;
    private final ModelMapper modelMapper;
    
    private static final Logger log =
            LoggerFactory.getLogger(EmployeeService.class);

    public EmployeeService(
            EmployeeRepository employeeRepository,
            CourseRepository courseRepository,
            CatalogSagaService catalogSagaService,
            ModelMapper modelMapper) {
        this.employeeRepository = employeeRepository;
        this.courseRepository = courseRepository;
        this.catalogSagaService = catalogSagaService;
        this.modelMapper = modelMapper;
    }

    public String addEmployee(EmployeeRequestDTO requestDTO) {
        log.debug("Mapping EmployeeRequestDTO to Employee entity");
        Employee employee = modelMapper.map(requestDTO, Employee.class);
        log.debug("Saving employee entity");
        Employee savedEmployee = employeeRepository.save(employee);
        if(Objects.isNull(savedEmployee) || Objects.isNull(savedEmployee.getId()))
            throw new BadRequestException("Error adding employee");
        log.info("Employee created with id={}", savedEmployee.getId());
        return savedEmployee.toString();
    }

    public String updateEmployee(Long id, EmployeeRequestDTO requestDTO) {
        log.debug("Retrieving employee with ID={}", id);
        Employee existingEmployee = employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if(Objects.isNull(existingEmployee))
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        else
        {
            log.debug("Updating employee with ID={}", id);
            if(Objects.nonNull(requestDTO.getName()))
                existingEmployee.setName(requestDTO.getName());
            if(Objects.nonNull(requestDTO.getEmail()))
                existingEmployee.setEmail(requestDTO.getEmail());
            if(Objects.nonNull(requestDTO.getRole()))
                existingEmployee.setRole(requestDTO.getRole());
            if(Objects.nonNull(requestDTO.getDepartment()))
                existingEmployee.setDepartment(requestDTO.getDepartment());
            if(Objects.nonNull(requestDTO.getSalary()))
                existingEmployee.setSalary(requestDTO.getSalary());
            if(Objects.nonNull(requestDTO.getExperience()))
                existingEmployee.setExperience(requestDTO.getExperience());
            Employee updatedEmployee = employeeRepository.save(existingEmployee);
            if(Objects.isNull(updatedEmployee) || Objects.isNull(updatedEmployee.getId()))
                return "Error updating employee";
            log.info("Employee updated with id={}", updatedEmployee.getId());
            return updatedEmployee.toString();
        }
    }

    public String deleteEmployee(Long id) {
        log.debug("Deleting employee via saga, id={}", id);
        return catalogSagaService.deleteEmployee(id);
    }

    public EmployeeResponseDTO getEmployee(Long id) {
        log.debug("Retrieving employee with ID={}", id);
        Employee existingEmployee = employeeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + id));
        if(Objects.isNull(existingEmployee))
            throw new ResourceNotFoundException("Employee not found with id: " + id);
        log.info("Employee retrieved with id={}", existingEmployee.getId());
        return modelMapper.map(existingEmployee, EmployeeResponseDTO.class);
    }

    public Page<EmployeeResponseDTO> getAllEmployees(Pageable pageable) {
        log.debug("Retrieving all employees with pagination");
        Page<Employee> employeePage = employeeRepository.findAll(pageable);
        log.info("Retrieved {} employees", employeePage.getNumberOfElements());
        return employeePage.map(employee -> modelMapper.map(employee, EmployeeResponseDTO.class));
    }

    public void setRecordsInPage(int size) {
        log.debug("Setting records in page to {}", size);
        PageSettings.setEmployeePageSize(size);
    }

    public List<CourseResponseDTO> getCoursesByEmployeeId(Long employeeId) {
        log.debug("Retrieving courses for employee ID={}", employeeId);
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        List<Course> courses = courseRepository.findByContentCreatorId(employeeId);
        log.info("Retrieved {} courses for employee id={}", courses.size(), employeeId);
        return courses.stream()
                .map(course -> modelMapper.map(course, CourseResponseDTO.class))
                .toList();
    }
}
