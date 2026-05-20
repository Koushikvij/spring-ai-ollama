package com.koushik.course_catalog.common.dbconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.koushik.course_catalog.employee.repository",
    entityManagerFactoryRef = "employeeEntityManager",
    transactionManagerRef = "employeeTransactionManager"
)
public class EmployeeDBConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.employee")
    public DataSourceProperties employeeDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource employeeDataSource(
            @Qualifier("employeeDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean employeeEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("employeeDataSource") DataSource employeeDataSource) {
        return builder
                .dataSource(employeeDataSource)
                .packages("com.koushik.course_catalog.employee.entity")
                .persistenceUnit("employeePU")
                .build();
    }

    @Bean
    public PlatformTransactionManager employeeTransactionManager(
            @Qualifier("employeeEntityManager") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
