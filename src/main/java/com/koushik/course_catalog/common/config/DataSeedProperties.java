package com.koushik.course_catalog.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.data.seed")
public class DataSeedProperties {

    private boolean enabled = false;
    private boolean onlyIfEmpty = true;
    private int employees = 20;
    private int customers = 50;
    private int courses = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isOnlyIfEmpty() {
        return onlyIfEmpty;
    }

    public void setOnlyIfEmpty(boolean onlyIfEmpty) {
        this.onlyIfEmpty = onlyIfEmpty;
    }

    public int getEmployees() {
        return employees;
    }

    public void setEmployees(int employees) {
        this.employees = employees;
    }

    public int getCustomers() {
        return customers;
    }

    public void setCustomers(int customers) {
        this.customers = customers;
    }

    public int getCourses() {
        return courses;
    }

    public void setCourses(int courses) {
        this.courses = courses;
    }
}
