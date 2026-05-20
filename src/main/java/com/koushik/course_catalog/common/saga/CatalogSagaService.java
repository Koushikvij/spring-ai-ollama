package com.koushik.course_catalog.common.saga;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.koushik.course_catalog.common.exception.BadRequestException;
import com.koushik.course_catalog.common.exception.ResourceNotFoundException;
import com.koushik.course_catalog.course.dto.CourseRequestDTO;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@Service
public class CatalogSagaService {

    private static final Logger log = LoggerFactory.getLogger(CatalogSagaService.class);

    static final String KEY_COURSE = "course";
    static final String KEY_COURSE_ID = "courseId";
    static final String KEY_CUSTOMER_ID = "customerId";
    static final String KEY_EMPLOYEE_ID = "employeeId";
    static final String KEY_CREATOR_IDS = "creatorIds";
    static final String KEY_CUSTOMER_IDS = "customerIds";
    static final String KEY_LINKED_CUSTOMER_IDS = "linkedCustomerIds";
    static final String KEY_CUSTOMER_SNAPSHOTS = "customerSnapshots";
    static final String KEY_COURSE_SNAPSHOTS = "courseSnapshots";
    static final String KEY_REQUEST = "request";

    private final SagaOrchestrator sagaOrchestrator;
    private final CourseRepository courseRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final ModelMapper modelMapper;

    public CatalogSagaService(
            SagaOrchestrator sagaOrchestrator,
            CourseRepository courseRepository,
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            ModelMapper modelMapper) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.courseRepository = courseRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.modelMapper = modelMapper;
    }

    public String createCourse(CourseRequestDTO requestDTO) {
        SagaContext context = new SagaContext();
        context.put(KEY_REQUEST, requestDTO);
        context.put(KEY_CREATOR_IDS, safeIds(requestDTO.getContentCreatorIds()));
        context.put(KEY_CUSTOMER_IDS, safeIds(requestDTO.getEnrolledCustomerIds()));
        context.put(KEY_LINKED_CUSTOMER_IDS, new ArrayList<Long>());

        sagaOrchestrator.execute("CreateCourse", List.of(
                step("ValidateCreators", this::validateCreatorsExist, null),
                step("ValidateCustomers", this::validateCustomersExist, null),
                step("PersistCourse", this::persistCourse, this::compensatePersistCourse),
                step("LinkCustomers", this::linkCustomersToCourse, this::compensateLinkCustomers)), context);

        return context.<Course>get(KEY_COURSE).toString();
    }

    public String updateCourse(Long courseId, CourseRequestDTO requestDTO) {
        Course existing = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (Objects.nonNull(requestDTO.getTitle())) {
            existing.setTitle(requestDTO.getTitle());
        }
        if (Objects.nonNull(requestDTO.getDescription())) {
            existing.setDescription(requestDTO.getDescription());
        }
        if (Objects.nonNull(requestDTO.getContentCreatorIds())) {
            assignCreators(courseId, requestDTO.getContentCreatorIds());
            existing = courseRepository.findById(courseId).orElseThrow();
        }
        if (Objects.nonNull(requestDTO.getEnrolledCustomerIds())) {
            syncCourseEnrollment(courseId, safeIds(requestDTO.getEnrolledCustomerIds()));
            existing = courseRepository.findById(courseId).orElseThrow();
        } else {
            courseRepository.save(existing);
        }
        return existing.toString();
    }

    public String deleteCourse(Long courseId) {
        SagaContext context = new SagaContext();
        context.put(KEY_COURSE_ID, courseId);

        sagaOrchestrator.execute("DeleteCourse", List.of(
                step("LoadCourse", this::loadCourseForDeletion, null),
                step("UnlinkCustomers", this::unlinkCustomersFromCourse, this::compensateUnlinkCustomersFromCourse),
                step("DeleteCourse", ctx -> courseRepository.deleteById(courseId), this::compensateDeleteCourse)), context);

        return "Course deleted successfully";
    }

    public String enrollCustomer(Long courseId, Long customerId) {
        SagaContext context = new SagaContext();
        context.put(KEY_COURSE_ID, courseId);
        context.put(KEY_CUSTOMER_ID, customerId);

        sagaOrchestrator.execute("EnrollCustomer", enrollmentSteps(), context);
        return "Customer " + customerId + " enrolled in course " + courseId;
    }

    public String unenrollCustomer(Long courseId, Long customerId) {
        SagaContext context = new SagaContext();
        context.put(KEY_COURSE_ID, courseId);
        context.put(KEY_CUSTOMER_ID, customerId);

        sagaOrchestrator.execute("UnenrollCustomer", List.of(
                step("ValidateEntities", this::validateCourseAndCustomerExist, null),
                step("RemoveFromCourse", this::removeCustomerFromCourse, this::compensateRestoreCourseEnrollment),
                step("RemoveFromCustomer", this::removeCourseFromCustomer, this::compensateRestoreCustomerEnrollment)), context);

        return "Customer " + customerId + " unenrolled from course " + courseId;
    }

    public String assignCreators(Long courseId, List<Long> creatorIds) {
        SagaContext context = new SagaContext();
        context.put(KEY_COURSE_ID, courseId);
        context.put(KEY_CREATOR_IDS, safeIds(creatorIds));

        sagaOrchestrator.execute("AssignCreators", List.of(
                step("ValidateCreators", this::validateCreatorsExistForCourse, null),
                step("UpdateCreators", this::updateCourseCreators, this::compensateRestoreCourseCreators)), context);

        return "Creators assigned to course " + courseId;
    }

    public String deleteCustomer(Long customerId) {
        SagaContext context = new SagaContext();
        context.put(KEY_CUSTOMER_ID, customerId);

        sagaOrchestrator.execute("DeleteCustomer", List.of(
                step("LoadCustomer", this::loadCustomerForDeletion, null),
                step("UnlinkFromCourses", this::unlinkCustomerFromAllCourses, this::compensateRestoreCourseEnrollment),
                step("DeleteCustomer", ctx -> customerRepository.deleteById(customerId), this::compensateDeleteCustomer)), context);

        return "Customer deleted successfully";
    }

    public String deleteEmployee(Long employeeId) {
        SagaContext context = new SagaContext();
        context.put(KEY_EMPLOYEE_ID, employeeId);

        sagaOrchestrator.execute("DeleteEmployee", List.of(
                step("LoadEmployee", this::loadEmployeeForDeletion, null),
                step("UnlinkFromCourses", this::unlinkEmployeeFromAllCourses, this::compensateRestoreCourseCreators),
                step("DeleteEmployee", ctx -> employeeRepository.deleteById(employeeId), this::compensateDeleteEmployee)), context);

        return "Employee deleted successfully";
    }

    private List<SagaStep> enrollmentSteps() {
        return List.of(
                step("ValidateEntities", this::validateCourseAndCustomerExist, null),
                step("AddToCourse", this::addCustomerToCourse, this::compensateRestoreCourseEnrollment),
                step("AddToCustomer", this::addCourseToCustomer, this::compensateRestoreCustomerEnrollment));
    }

    private void syncCourseEnrollment(Long courseId, List<Long> targetCustomerIds) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Set<Long> current = new HashSet<>(safeIds(course.getEnrolledCustomerIds()));
        Set<Long> target = new HashSet<>(targetCustomerIds);

        for (Long customerId : new HashSet<>(current)) {
            if (!target.contains(customerId)) {
                unenrollCustomer(courseId, customerId);
            }
        }
        for (Long customerId : target) {
            if (!current.contains(customerId)) {
                enrollCustomer(courseId, customerId);
            }
        }
    }

    private SagaStep step(String name, SagaAction action, SagaAction compensation) {
        return new SagaStep() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void run(SagaContext context) throws Exception {
                action.accept(context);
            }

            @Override
            public void compensate(SagaContext context) {
                if (compensation != null) {
                    try {
                        compensation.accept(context);
                    } catch (Exception ex) {
                        log.warn("Compensation step [{}] failed", name, ex);
                    }
                }
            }
        };
    }

    private void validateCreatorsExist(SagaContext context) throws Exception {
        validateEmployeeIds(context.get(KEY_CREATOR_IDS));
    }

    private void validateCustomersExist(SagaContext context) throws Exception {
        List<Long> customerIds = context.get(KEY_CUSTOMER_IDS);
        if (customerIds.isEmpty()) {
            return;
        }
        List<Customer> found = customerRepository.findAllById(customerIds);
        if (found.size() != customerIds.size()) {
            throw new BadRequestException("One or more customer IDs do not exist");
        }
    }

    private void persistCourse(SagaContext context) throws Exception {
        CourseRequestDTO request = context.get(KEY_REQUEST);
        Course course = modelMapper.map(request, Course.class);
        course.setEnrolledCustomerIds(new ArrayList<>());
        Course saved = courseRepository.save(course);
        context.put(KEY_COURSE, saved);
        context.put(KEY_COURSE_ID, saved.getId());
    }

    private void compensatePersistCourse(SagaContext context) {
        Long courseId = context.get(KEY_COURSE_ID);
        if (courseId != null) {
            courseRepository.deleteById(courseId);
        }
    }

    private void linkCustomersToCourse(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        List<Long> customerIds = context.get(KEY_CUSTOMER_IDS);
        List<Long> linked = context.get(KEY_LINKED_CUSTOMER_IDS);

        for (Long customerId : customerIds) {
            SagaContext enrollContext = new SagaContext();
            enrollContext.put(KEY_COURSE_ID, courseId);
            enrollContext.put(KEY_CUSTOMER_ID, customerId);
            sagaOrchestrator.execute("EnrollCustomerNested", enrollmentSteps(), enrollContext);
            linked.add(customerId);
        }
    }

    private void compensateLinkCustomers(SagaContext context) {
        Long courseId = context.get(KEY_COURSE_ID);
        List<Long> linked = context.get(KEY_LINKED_CUSTOMER_IDS);
        if (linked == null) {
            return;
        }
        for (Long customerId : new ArrayList<>(linked)) {
            try {
                SagaContext unenrollContext = new SagaContext();
                unenrollContext.put(KEY_COURSE_ID, courseId);
                unenrollContext.put(KEY_CUSTOMER_ID, customerId);
                removeCustomerFromCourse(unenrollContext);
                removeCourseFromCustomer(unenrollContext);
            } catch (Exception ex) {
                log.warn("Failed to compensate enrollment for customer {} in course {}", customerId, courseId, ex);
            }
        }
    }

    private void validateCreatorsExistForCourse(SagaContext context) throws Exception {
        validateEmployeeIds(context.get(KEY_CREATOR_IDS));
        courseRepository.findById(context.get(KEY_COURSE_ID))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + context.get(KEY_COURSE_ID)));
    }

    private void updateCourseCreators(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        List<Long> creatorIds = context.get(KEY_CREATOR_IDS);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        context.put(KEY_COURSE_SNAPSHOTS, Map.of(courseId, LinkIdHelper.copyIds(course.getContentCreatorIds())));
        course.setContentCreatorIds(new ArrayList<>(creatorIds));
        courseRepository.save(course);
    }

    private void loadCourseForDeletion(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        context.put(KEY_COURSE, course);
        context.put(KEY_CUSTOMER_IDS, LinkIdHelper.copyIds(course.getEnrolledCustomerIds()));
    }

    private void unlinkCustomersFromCourse(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        List<Long> customerIds = context.get(KEY_CUSTOMER_IDS);
        Map<Long, List<Long>> snapshots = new HashMap<>();
        context.put(KEY_CUSTOMER_SNAPSHOTS, snapshots);

        for (Long customerId : customerIds) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null) {
                continue;
            }
            snapshots.put(customerId, LinkIdHelper.copyIds(customer.getEnrolledCourseIds()));
            List<Long> enrolled = LinkIdHelper.copyIds(customer.getEnrolledCourseIds());
            LinkIdHelper.removeId(enrolled, courseId);
            customer.setEnrolledCourseIds(enrolled);
            customerRepository.save(customer);
        }
    }

    private void compensateUnlinkCustomersFromCourse(SagaContext context) {
        compensateRestoreCustomerEnrollment(context);
    }

    private void compensateDeleteCourse(SagaContext context) {
        Course course = context.get(KEY_COURSE);
        if (course != null) {
            courseRepository.save(course);
        }
    }

    private void validateCourseAndCustomerExist(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Long customerId = context.get(KEY_CUSTOMER_ID);
        courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private void addCustomerToCourse(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Long customerId = context.get(KEY_CUSTOMER_ID);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        context.put(KEY_COURSE_SNAPSHOTS, Map.of(courseId, LinkIdHelper.copyIds(course.getEnrolledCustomerIds())));
        List<Long> enrolled = LinkIdHelper.copyIds(course.getEnrolledCustomerIds());
        LinkIdHelper.addId(enrolled, customerId);
        course.setEnrolledCustomerIds(enrolled);
        courseRepository.save(course);
    }

    private void addCourseToCustomer(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Long customerId = context.get(KEY_CUSTOMER_ID);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        context.put(KEY_CUSTOMER_SNAPSHOTS, Map.of(customerId, LinkIdHelper.copyIds(customer.getEnrolledCourseIds())));
        List<Long> courses = LinkIdHelper.copyIds(customer.getEnrolledCourseIds());
        LinkIdHelper.addId(courses, courseId);
        customer.setEnrolledCourseIds(courses);
        customerRepository.save(customer);
    }

    private void removeCustomerFromCourse(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Long customerId = context.get(KEY_CUSTOMER_ID);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        context.put(KEY_COURSE_SNAPSHOTS, Map.of(courseId, LinkIdHelper.copyIds(course.getEnrolledCustomerIds())));
        List<Long> enrolled = LinkIdHelper.copyIds(course.getEnrolledCustomerIds());
        LinkIdHelper.removeId(enrolled, customerId);
        course.setEnrolledCustomerIds(enrolled);
        courseRepository.save(course);
    }

    private void removeCourseFromCustomer(SagaContext context) throws Exception {
        Long courseId = context.get(KEY_COURSE_ID);
        Long customerId = context.get(KEY_CUSTOMER_ID);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        context.put(KEY_CUSTOMER_SNAPSHOTS, Map.of(customerId, LinkIdHelper.copyIds(customer.getEnrolledCourseIds())));
        List<Long> courses = LinkIdHelper.copyIds(customer.getEnrolledCourseIds());
        LinkIdHelper.removeId(courses, courseId);
        customer.setEnrolledCourseIds(courses);
        customerRepository.save(customer);
    }

    private void loadCustomerForDeletion(SagaContext context) throws Exception {
        Long customerId = context.get(KEY_CUSTOMER_ID);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        context.put("customer", customer);
    }

    private void unlinkCustomerFromAllCourses(SagaContext context) throws Exception {
        Long customerId = context.get(KEY_CUSTOMER_ID);
        List<Course> courses = courseRepository.findByEnrolledCustomerId(customerId);
        Map<Long, List<Long>> snapshots = new HashMap<>();
        context.put(KEY_COURSE_SNAPSHOTS, snapshots);

        for (Course course : courses) {
            snapshots.put(course.getId(), LinkIdHelper.copyIds(course.getEnrolledCustomerIds()));
            List<Long> enrolled = LinkIdHelper.copyIds(course.getEnrolledCustomerIds());
            LinkIdHelper.removeId(enrolled, customerId);
            course.setEnrolledCustomerIds(enrolled);
            courseRepository.save(course);
        }
    }

    private void compensateDeleteCustomer(SagaContext context) {
        Customer customer = context.get("customer");
        if (customer != null) {
            customerRepository.save(customer);
        }
    }

    private void loadEmployeeForDeletion(SagaContext context) throws Exception {
        Long employeeId = context.get(KEY_EMPLOYEE_ID);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
        context.put("employee", employee);
    }

    private void unlinkEmployeeFromAllCourses(SagaContext context) throws Exception {
        Long employeeId = context.get(KEY_EMPLOYEE_ID);
        List<Course> courses = courseRepository.findByContentCreatorId(employeeId);
        Map<Long, List<Long>> snapshots = new HashMap<>();
        context.put(KEY_COURSE_SNAPSHOTS, snapshots);

        for (Course course : courses) {
            snapshots.put(course.getId(), LinkIdHelper.copyIds(course.getContentCreatorIds()));
            List<Long> creators = LinkIdHelper.copyIds(course.getContentCreatorIds());
            LinkIdHelper.removeId(creators, employeeId);
            course.setContentCreatorIds(creators);
            courseRepository.save(course);
        }
    }

    private void compensateDeleteEmployee(SagaContext context) {
        Employee employee = context.get("employee");
        if (employee != null) {
            employeeRepository.save(employee);
        }
    }

    private void compensateRestoreCourseEnrollment(SagaContext context) {
        Map<Long, List<Long>> snapshots = context.get(KEY_COURSE_SNAPSHOTS);
        if (snapshots == null) {
            return;
        }
        snapshots.forEach((courseId, previousIds) -> courseRepository.findById(courseId).ifPresent(course -> {
            course.setEnrolledCustomerIds(new ArrayList<>(previousIds));
            courseRepository.save(course);
        }));
    }

    private void compensateRestoreCustomerEnrollment(SagaContext context) {
        Map<Long, List<Long>> snapshots = context.get(KEY_CUSTOMER_SNAPSHOTS);
        if (snapshots == null) {
            return;
        }
        snapshots.forEach((customerId, previousIds) -> customerRepository.findById(customerId).ifPresent(customer -> {
            customer.setEnrolledCourseIds(new ArrayList<>(previousIds));
            customerRepository.save(customer);
        }));
    }

    private void compensateRestoreCourseCreators(SagaContext context) {
        Map<Long, List<Long>> snapshots = context.get(KEY_COURSE_SNAPSHOTS);
        if (snapshots == null) {
            return;
        }
        snapshots.forEach((courseId, previousIds) -> courseRepository.findById(courseId).ifPresent(course -> {
            course.setContentCreatorIds(new ArrayList<>(previousIds));
            courseRepository.save(course);
        }));
    }

    private void validateEmployeeIds(List<Long> employeeIds) {
        if (employeeIds == null || employeeIds.isEmpty()) {
            throw new BadRequestException("At least one content creator ID is required");
        }
        List<Employee> found = employeeRepository.findAllById(employeeIds);
        if (found.size() != employeeIds.size()) {
            throw new BadRequestException("One or more employee IDs do not exist");
        }
    }

    private List<Long> safeIds(List<Long> ids) {
        return ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    @FunctionalInterface
    private interface SagaAction {
        void accept(SagaContext context) throws Exception;
    }
}
