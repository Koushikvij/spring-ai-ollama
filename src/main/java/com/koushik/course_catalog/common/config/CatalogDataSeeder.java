package com.koushik.course_catalog.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.koushik.course_catalog.common.saga.LinkIdHelper;
import com.koushik.course_catalog.course.entity.Course;
import com.koushik.course_catalog.course.repository.CourseRepository;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;
import com.koushik.course_catalog.employee.entity.Employee;
import com.koushik.course_catalog.employee.repository.EmployeeRepository;

@Component
@EnableConfigurationProperties(DataSeedProperties.class)
public class CatalogDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private static final String[] FIRST_NAMES = {
            "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Jamie", "Quinn",
            "Avery", "Reese", "Sam", "Dakota", "Skyler", "Rowan", "Emerson", "Hayden"
    };
    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas"
    };
    private static final String[] ROLES = {
            "Instructor", "Senior Instructor", "Content Creator", "Curriculum Lead", "Teaching Assistant"
    };
    private static final String[] DEPARTMENTS = {
            "Engineering", "Content", "Education", "Product", "Operations"
    };
    private static final String[] COURSE_TOPICS = {
            "Java Fundamentals", "Spring Boot", "Microservices", "REST API Design", "PostgreSQL",
            "Docker", "Kubernetes", "JUnit Testing", "Hibernate JPA", "System Design",
            "Git & CI/CD", "Security Basics", "Reactive Programming", "Cloud Native Apps",
            "Design Patterns", "Clean Code", "Agile Delivery", "Kafka Messaging", "Redis Caching",
            "OAuth2", "GraphQL", "Angular Basics", "React Basics", "Python for Data",
            "Machine Learning Intro", "DevOps Pipelines", "Linux Administration", "Networking 101",
            "SQL Advanced", "NoSQL Overview"
    };

    private final DataSeedProperties properties;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final CourseRepository courseRepository;

    public CatalogDataSeeder(
            DataSeedProperties properties,
            EmployeeRepository employeeRepository,
            CustomerRepository customerRepository,
            CourseRepository courseRepository) {
        this.properties = properties;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        if (!properties.isEnabled()) {
            log.info("Data seeding is disabled (app.data.seed.enabled=false)");
            return;
        }

        if (properties.isOnlyIfEmpty() && hasExistingData()) {
            log.info("Data seeding skipped: databases already contain records (app.data.seed.only-if-empty=true)");
            return;
        }

        log.info("Starting data seed: {} employees, {} customers, {} courses",
                properties.getEmployees(), properties.getCustomers(), properties.getCourses());

        List<Employee> employees = seedEmployees(properties.getEmployees());
        List<Customer> customers = seedCustomers(properties.getCustomers());
        seedCourses(properties.getCourses(), employees, customers);

        log.info("Data seed completed successfully");
    }

    private boolean hasExistingData() {
        return employeeRepository.count() > 0
                || customerRepository.count() > 0
                || courseRepository.count() > 0;
    }

    private List<Employee> seedEmployees(int count) {
        List<Employee> employees = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 1; i <= count; i++) {
            Employee employee = new Employee();
            employee.setName(randomName(random));
            employee.setEmail("employee" + i + "@course-catalog.test");
            employee.setRole(ROLES[random.nextInt(ROLES.length)]);
            employee.setDepartment(DEPARTMENTS[random.nextInt(DEPARTMENTS.length)]);
            employee.setSalary(55_000d + random.nextInt(95_000));
            employee.setExperience(1d + random.nextInt(20));
            employees.add(employeeRepository.save(employee));
        }

        log.info("Seeded {} employees", employees.size());
        return employees;
    }

    private List<Customer> seedCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 1; i <= count; i++) {
            Customer customer = new Customer();
            customer.setName(randomName(random));
            customer.setEmail("customer" + i + "@course-catalog.test");
            customer.setPhone("+1-555-%04d".formatted(1000 + i));
            customer.setEnrolledCourseIds(new ArrayList<>());
            customers.add(customerRepository.save(customer));
        }

        log.info("Seeded {} customers", customers.size());
        return customers;
    }

    private void seedCourses(int count, List<Employee> employees, List<Customer> customers) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Long> employeeIds = employees.stream().map(Employee::getId).toList();
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();

        for (int i = 1; i <= count; i++) {
            Course course = new Course();
            String topic = COURSE_TOPICS[(i - 1) % COURSE_TOPICS.length];
            course.setTitle(topic + " (" + i + ")");
            course.setDescription("Dummy course covering " + topic + ". Auto-generated seed data.");
            course.setContentCreatorIds(pickRandomIds(employeeIds, 1, 3, random));
            course.setEnrolledCustomerIds(new ArrayList<>());
            Course saved = courseRepository.save(course);

            List<Long> enrolledCustomerIds = pickRandomIds(customerIds, 3, 12, random);
            linkEnrollment(saved.getId(), enrolledCustomerIds, customers);
        }

        log.info("Seeded {} courses with enrollment links", count);
    }

    private void linkEnrollment(Long courseId, List<Long> customerIdsToEnroll, List<Customer> allCustomers) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Long> enrolledOnCourse = LinkIdHelper.copyIds(course.getEnrolledCustomerIds());

        for (Long customerId : customerIdsToEnroll) {
            LinkIdHelper.addId(enrolledOnCourse, customerId);
            allCustomers.stream()
                    .filter(c -> c.getId().equals(customerId))
                    .findFirst()
                    .ifPresent(customer -> {
                        List<Long> courses = LinkIdHelper.copyIds(customer.getEnrolledCourseIds());
                        LinkIdHelper.addId(courses, courseId);
                        customer.setEnrolledCourseIds(courses);
                        customerRepository.save(customer);
                    });
        }

        course.setEnrolledCustomerIds(enrolledOnCourse);
        courseRepository.save(course);
    }

    private List<Long> pickRandomIds(List<Long> source, int min, int max, ThreadLocalRandom random) {
        if (source.isEmpty()) {
            return List.of();
        }
        int pickCount = Math.min(source.size(), min + random.nextInt(max - min + 1));
        List<Long> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled, random);
        return new ArrayList<>(shuffled.subList(0, pickCount));
    }

    private String randomName(ThreadLocalRandom random) {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }
}
