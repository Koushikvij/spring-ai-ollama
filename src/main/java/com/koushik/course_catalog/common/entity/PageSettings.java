package com.koushik.course_catalog.common.entity;

import lombok.Data;

@Data
public class PageSettings {
    private static int employeePagesize = 5;
    private static int customerPagesize = 5;
    private static int coursePagesize = 5;

    public PageSettings() {
        PageSettings.employeePagesize=5;
        PageSettings.customerPagesize=5;
        PageSettings.coursePagesize=5;
    }
    
    public PageSettings(int size) {
        PageSettings.employeePagesize = size;
        PageSettings.customerPagesize = size;
        PageSettings.coursePagesize = size;
    }

    public static int getEmployeePageSize() {
        return employeePagesize;
    }

    public static int getCustomerPageSize() {
        return customerPagesize;
    }

    public static int getCoursePageSize() {
        return coursePagesize;
    }

    public static void setEmployeePageSize(int size) {
        PageSettings.employeePagesize = size;
    }

    public static void setCustomerPageSize(int size) {
        PageSettings.customerPagesize = size;
    }

    public static void setCoursePageSize(int size) {
        PageSettings.coursePagesize = size;
    }
}
