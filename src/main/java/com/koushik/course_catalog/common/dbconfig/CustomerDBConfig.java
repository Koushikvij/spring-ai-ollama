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
    basePackages = "com.koushik.course_catalog.customer.repository",
    entityManagerFactoryRef = "customerEntityManager",
    transactionManagerRef = "customerTransactionManager"
)
public class CustomerDBConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.customer")
    public DataSourceProperties customerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource customerDataSource(
            @Qualifier("customerDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean customerEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("customerDataSource") DataSource customerDataSource) {
        return builder
                .dataSource(customerDataSource)
                .packages("com.koushik.course_catalog.customer.entity")
                .persistenceUnit("customerPU")
                .build();
    }

    @Bean
    public PlatformTransactionManager customerTransactionManager(
            @Qualifier("customerEntityManager") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
