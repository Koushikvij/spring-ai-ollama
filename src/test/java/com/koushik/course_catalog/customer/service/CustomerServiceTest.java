package com.koushik.course_catalog.customer.service;

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

import com.koushik.course_catalog.common.exception.BadRequestException;
import com.koushik.course_catalog.common.saga.CatalogSagaService;
import com.koushik.course_catalog.customer.dto.CustomerRequestDTO;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.customer.entity.Customer;
import com.koushik.course_catalog.customer.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CatalogSagaService catalogSagaService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, catalogSagaService, new ModelMapper());
    }

    @Test
    void addCustomer_savesAndReturnsEntityString() {
        CustomerRequestDTO request = new CustomerRequestDTO("Ann", "ann@test.com", "+1-555", List.of());
        Customer saved = new Customer(1L, "Ann", "ann@test.com", "+1-555", List.of());
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        String result = customerService.addCustomer(request);

        assertThat(result).contains("Ann");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void addCustomer_throwsWhenSaveReturnsNullId() {
        CustomerRequestDTO request = new CustomerRequestDTO("Ann", "ann@test.com", "+1-555", List.of());
        Customer saved = new Customer(null, "Ann", "ann@test.com", "+1-555", List.of());
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        assertThatThrownBy(() -> customerService.addCustomer(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getCustomer_returnsDto() {
        Customer customer = new Customer(1L, "Ann", "ann@test.com", "+1", List.of());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        CustomerResponseDTO dto = customerService.getCustomer(1L);

        assertThat(dto.getName()).isEqualTo("Ann");
    }

    @Test
    void deleteCustomer_delegatesToSaga() {
        when(catalogSagaService.deleteCustomer(1L)).thenReturn("Customer deleted successfully");

        assertThat(customerService.deleteCustomer(1L)).contains("deleted");
        verify(catalogSagaService).deleteCustomer(1L);
    }

    @Test
    void enrollInCourse_delegatesToSaga() {
        when(catalogSagaService.enrollCustomer(5L, 1L)).thenReturn("enrolled");

        assertThat(customerService.enrollInCourse(1L, 5L)).isEqualTo("enrolled");
        verify(catalogSagaService).enrollCustomer(5L, 1L);
    }
}
