package com.koushik.course_catalog.customer.service;

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
import com.koushik.course_catalog.customer.dto.CustomerRequestDTO;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;


@Service
public class CustomerService {
	private final CustomerRepository customerRepository;
	private final CatalogSagaService catalogSagaService;
	private final ModelMapper modelMapper;
    
	private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

	public CustomerService(
			CustomerRepository customerRepository,
			CatalogSagaService catalogSagaService,
			ModelMapper modelMapper) {
		this.customerRepository = customerRepository;
		this.catalogSagaService = catalogSagaService;
		this.modelMapper = modelMapper;
	}

	public String addCustomer(CustomerRequestDTO requestDTO) {
		log.debug("Mapping CustomerRequestDTO to Customer entity");
		Customer customer = modelMapper.map(requestDTO, Customer.class);
		log.debug("Saving customer entity");
		Customer savedCustomer = customerRepository.save(customer);
		if(Objects.isNull(savedCustomer) || Objects.isNull(savedCustomer.getId()))
			throw new BadRequestException("Error adding customer");
		log.info("Customer created with id={}", savedCustomer.getId());
		return savedCustomer.toString();
	}

	public String updateCustomer(Long id, CustomerRequestDTO requestDTO) {
		log.debug("Retrieving customer with ID={}", id);
		Customer existingCustomer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
		if(Objects.isNull(existingCustomer))
			throw new ResourceNotFoundException("Customer not found with id: " + id);
		else {
			log.debug("Updating customer with ID={}", id);
			if(Objects.nonNull(requestDTO.getName()))
				existingCustomer.setName(requestDTO.getName());
			if(Objects.nonNull(requestDTO.getEmail()))
				existingCustomer.setEmail(requestDTO.getEmail());
			if(Objects.nonNull(requestDTO.getPhone()))
				existingCustomer.setPhone(requestDTO.getPhone());
			if(Objects.nonNull(requestDTO.getEnrolledCourseIds()))
				existingCustomer.setEnrolledCourseIds(requestDTO.getEnrolledCourseIds());
			Customer updatedCustomer = customerRepository.save(existingCustomer);
			if(Objects.isNull(updatedCustomer) || Objects.isNull(updatedCustomer.getId()))
				return "Error updating customer";
			log.info("Customer updated with id={}", updatedCustomer.getId());
			return updatedCustomer.toString();
		}
	}

	public String deleteCustomer(Long id) {
		log.debug("Deleting customer via saga, id={}", id);
		return catalogSagaService.deleteCustomer(id);
	}

	public String enrollInCourse(Long customerId, Long courseId) {
		log.debug("Enrolling customer {} in course {} via saga", customerId, courseId);
		return catalogSagaService.enrollCustomer(courseId, customerId);
	}

	public CustomerResponseDTO getCustomer(Long id) {
		log.debug("Retrieving customer with ID={}", id);
		Customer existingCustomer = customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
		if(Objects.isNull(existingCustomer))
			throw new ResourceNotFoundException("Customer not found with id: " + id);
		log.info("Customer retrieved with id={}", existingCustomer.getId());
		return modelMapper.map(existingCustomer, CustomerResponseDTO.class);
	}

	public Page<CustomerResponseDTO> getAllCustomers(Pageable pageable) {
		log.debug("Retrieving all customers with pagination");
		Page<Customer> customerPage = customerRepository.findAll(pageable);
		log.info("Retrieved {} customers", customerPage.getNumberOfElements());
		return customerPage.map(customer -> modelMapper.map(customer, CustomerResponseDTO.class));
	}

	public void setRecordsInPage(int size) {
		log.debug("Setting records in page to {}", size);
		PageSettings.setCustomerPageSize(size);
	}
}
