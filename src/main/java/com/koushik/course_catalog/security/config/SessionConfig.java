package com.koushik.course_catalog.security.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 28800)
@Profile("!test")
public class SessionConfig {

    @Bean
    PersistentTokenRepository persistentTokenRepository(
            @Qualifier("courseDataSource") DataSource courseDataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(courseDataSource);
        repository.setCreateTableOnStartup(true);
        return repository;
    }

    @Bean(name = "springSessionTransactionOperations")
    TransactionOperations springSessionTransactionOperations(
            @Qualifier("courseTransactionManager") PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
