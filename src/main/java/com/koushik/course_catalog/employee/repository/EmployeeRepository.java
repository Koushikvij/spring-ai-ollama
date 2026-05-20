package com.koushik.course_catalog.employee.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koushik.course_catalog.employee.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

}
