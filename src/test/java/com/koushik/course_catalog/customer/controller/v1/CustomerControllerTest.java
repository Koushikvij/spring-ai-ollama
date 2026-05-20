package com.koushik.course_catalog.customer.controller.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.koushik.course_catalog.common.exception.GlobalExceptionHandler;
import com.koushik.course_catalog.customer.dto.CustomerResponseDTO;
import com.koushik.course_catalog.support.TestPageSettings;
import com.koushik.course_catalog.customer.service.CustomerService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        TestPageSettings.resetToDefault();
        CustomerController controller = new CustomerController(customerService, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAllCustomers_returnsPage() throws Exception {
        CustomerResponseDTO dto = new CustomerResponseDTO(1L, "Ann", "a@test.com", "+1", List.of());
        when(customerService.getAllCustomers(any())).thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/v1/customers/getAllCustomers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ann"));
    }

    @Test
    void addCustomer_returnsCreated() throws Exception {
        when(customerService.addCustomer(any())).thenReturn("Customer[id=1]");

        mockMvc.perform(post("/api/v1/customers/addCustomer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Ann",
                                  "email": "ann@test.com",
                                  "phone": "+1-555-1001"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void addCustomer_returnsBadRequestWhenValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/customers/addCustomer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "phone": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void enrollInCourse_returnsOk() throws Exception {
        when(customerService.enrollInCourse(1L, 5L)).thenReturn("enrolled");

        mockMvc.perform(post("/api/v1/customers/enrollInCourse/1/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("enrolled"));
    }
}
