package com.koushik.course_catalog.security.authorization;

import org.springframework.http.HttpMethod;

public final class RoleAccessRules {

    private RoleAccessRules() {
    }

    public static boolean isAllowed(String role, HttpMethod method, String path) {
        if ("ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role)) {
            return path.startsWith("/api/v1/");
        }
        if ("ROLE_EMPLOYEE".equals(role)) {
            return employeeAccess(method, path);
        }
        if ("ROLE_CUSTOMER".equals(role)) {
            return customerAccess(method, path);
        }
        return false;
    }

    private static boolean employeeAccess(HttpMethod method, String path) {
        if (!method.equals(HttpMethod.GET)) {
            return false;
        }
        return path.startsWith("/api/v1/courses/")
                || path.startsWith("/api/v1/employees/")
                || path.equals("/api/v1/auth/me")
                || path.equals("/api/v1/auth/logout");
    }

    private static boolean customerAccess(HttpMethod method, String path) {
        if (path.startsWith("/api/v1/auth/")) {
            return true;
        }
        if (method.equals(HttpMethod.GET)) {
            return path.startsWith("/api/v1/courses/")
                    || path.startsWith("/api/v1/customers/");
        }
        if (method.equals(HttpMethod.POST)) {
            return path.contains("/enrollCustomer/")
                    || path.contains("/enrollInCourse/");
        }
        if (method.equals(HttpMethod.DELETE)) {
            return path.contains("/unenrollCustomer/");
        }
        return false;
    }
}
