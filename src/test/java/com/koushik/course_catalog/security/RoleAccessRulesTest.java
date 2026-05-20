package com.koushik.course_catalog.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import com.koushik.course_catalog.security.authorization.RoleAccessRules;

class RoleAccessRulesTest {

    @Test
    void adminCanAccessWriteEndpoints() {
        assertThat(RoleAccessRules.isAllowed("ROLE_ADMIN", HttpMethod.DELETE, "/api/v1/courses/deleteCourse/1"))
                .isTrue();
    }

    @Test
    void employeeCanOnlyReadCoursesAndEmployees() {
        assertThat(RoleAccessRules.isAllowed("ROLE_EMPLOYEE", HttpMethod.GET, "/api/v1/courses/getAllCourses"))
                .isTrue();
        assertThat(RoleAccessRules.isAllowed("ROLE_EMPLOYEE", HttpMethod.POST, "/api/v1/courses/addCourse"))
                .isFalse();
    }

    @Test
    void customerCanReadAndEnroll() {
        assertThat(RoleAccessRules.isAllowed("ROLE_CUSTOMER", HttpMethod.GET, "/api/v1/customers/getCustomer/1"))
                .isTrue();
        assertThat(RoleAccessRules.isAllowed("ROLE_CUSTOMER", HttpMethod.POST, "/api/v1/courses/enrollCustomer/1/2"))
                .isTrue();
        assertThat(RoleAccessRules.isAllowed("ROLE_CUSTOMER", HttpMethod.DELETE, "/api/v1/customers/deleteCustomer/1"))
                .isFalse();
    }
}
