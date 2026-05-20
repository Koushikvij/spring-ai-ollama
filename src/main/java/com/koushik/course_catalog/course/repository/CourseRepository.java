package com.koushik.course_catalog.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.koushik.course_catalog.course.entity.Course;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT c FROM Course c WHERE :employeeId MEMBER OF c.contentCreatorIds")
    List<Course> findByContentCreatorId(@Param("employeeId") Long employeeId);

    @Query("SELECT c FROM Course c WHERE :customerId MEMBER OF c.enrolledCustomerIds")
    List<Course> findByEnrolledCustomerId(@Param("customerId") Long customerId);
}
