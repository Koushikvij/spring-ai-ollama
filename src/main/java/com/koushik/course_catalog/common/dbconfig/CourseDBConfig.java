package com.koushik.course_catalog.common.dbconfig;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.koushik.course_catalog.course.repository",
    entityManagerFactoryRef = "courseEntityManager",
    transactionManagerRef = "courseTransactionManager"
)
public class CourseDBConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.course")
    public DataSourceProperties courseDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = {"courseDataSource", "dataSource"})
    @Primary
    public DataSource courseDataSource(
            @Qualifier("courseDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean courseEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("courseDataSource") DataSource courseDataSource) {
        return builder
                .dataSource(courseDataSource)
                .packages("com.koushik.course_catalog.course.entity")
                .persistenceUnit("coursePU")
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager courseTransactionManager(
            @Qualifier("courseEntityManager") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
