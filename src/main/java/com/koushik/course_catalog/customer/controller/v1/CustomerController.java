package com.koushik.course_catalog.customer.controller.v1;

import com.koushik.course_catalog.common.entity.PageSettings;
import com.koushik.course_catalog.customer.dto.CustomerRequestDTO;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.customer.service.CustomerService;
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
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer APIs", description = "CRUD + Pagination APIs for Customers")
public class CustomerController {

	private static final Logger log = LoggerFactory.getLogger(CustomerController.class);
	private final CustomerService customerService;

	public CustomerController(CustomerService customerService, ModelMapper modelMapper) {
		this.customerService = customerService;
	}

	@PostMapping("addCustomer")
	@Operation(summary = "Add a new customer", description = "Creates a new customer record in the system")
	public ResponseEntity<String> addCustomer(@Valid @RequestBody CustomerRequestDTO requestDTO) {
		log.info("Creating new customer with email={}", requestDTO.getEmail());
		String customer = customerService.addCustomer(requestDTO);
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body("Customer added successfully " + customer);
	}

	@PutMapping("updateCustomer/{id}")
	@Operation(summary = "Update an existing customer", description = "Updates the details of an existing customer by ID")
	public ResponseEntity<String> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequestDTO requestDTO) {
		log.info("Updating customer with ID={}", id);
		String response = customerService.updateCustomer(id, requestDTO);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body("Customer updated successfully " + response);
	}

	@DeleteMapping("deleteCustomer/{id}")
	@Operation(summary = "Delete a customer", description = "Deletes a customer record from the system by ID")
	public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {
		log.info("Deleting customer with ID={}", id);
		String response = customerService.deleteCustomer(id);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body("Customer deleted successfully " + response);
	}

	@GetMapping("getCustomer/{id}")
	@Operation(summary = "Get customer details", description = "Retrieves the details of a customer by ID")
	public ResponseEntity<String> getCustomer(@PathVariable Long id) {
		log.info("Retrieving customer with ID={}", id);
		CustomerResponseDTO response = customerService.getCustomer(id);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body("Customer retrieved successfully: " + response);
	}

	@GetMapping("getAllCustomers")
	@Operation(summary = "Get all customers with pagination", description = "Retrieves a paginated list of all customers, with optional sorting")
	public ResponseEntity<Page<CustomerResponseDTO>> getAllCustomers(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "id") String sortBy,
			@RequestParam(defaultValue = "asc") String sortDir
	) {
		log.info("Retrieving all customers");
		Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
		int size = PageSettings.getCustomerPageSize();
		Pageable pageable = PageRequest.of(page, size, sort);
		Page<CustomerResponseDTO> customerPage = customerService.getAllCustomers(pageable);
		return ResponseEntity
				.status(HttpStatus.OK)
				.body(customerPage);
	}

	@PostMapping("setRecordsinPage")
	@Operation(summary = "Set the number of records to display per page", description = "Configures the number of customer records to show on each page of the results")
	public void postMethodName(@RequestParam int size) {
		log.info("Setting records in page to {}", size);
		customerService.setRecordsInPage(size);
	}

	@PostMapping("enrollInCourse/{customerId}/{courseId}")
	@Operation(summary = "Enroll customer in course (saga)", description = "Atomically links customer and course enrollment across both databases with compensation on failure")
	public ResponseEntity<String> enrollInCourse(@PathVariable Long customerId, @PathVariable Long courseId) {
		log.info("Enrolling customer {} in course {} via saga", customerId, courseId);
		String result = customerService.enrollInCourse(customerId, courseId);
		return ResponseEntity.ok(result);
	}
}
