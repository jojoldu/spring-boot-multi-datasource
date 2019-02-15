package com.jojoldu.blogcode.datasource.web.config;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Qualifier;
import javax.sql.DataSource;

import java.util.HashMap;
import java.util.Map;

import static com.jojoldu.blogcode.datasource.web.config.LegacyDatasourceConfig.DOMAIN_PACKAGE;
import static com.jojoldu.blogcode.datasource.web.config.LegacyDatasourceConfig.ENTITY_MANAGER;
import static com.jojoldu.blogcode.datasource.web.config.LegacyDatasourceConfig.TX_MANAGER;

/**
 * JPA를 분리해서 쓰기 위해선 3개가 필요
 * Datasource
 * EntityManager
 * TransactionManager
 */
@Configuration
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(basePackages = DOMAIN_PACKAGE, entityManagerFactoryRef = ENTITY_MANAGER, transactionManagerRef = TX_MANAGER)
public class LegacyDatasourceConfig {

    public static final String DOMAIN_PACKAGE = "com.jojoldu.blogcode.datasource.core.legacy";
    public static final String ENTITY_MANAGER = "legacyEntityManager";
    public static final String TX_MANAGER = "legacyTxManager";
    public static final String DATA_SOURCE = "legacyDataSource";
    public static final String VENDOR_ADAPTER = "legacyJpaVendorAdapter";
    public static final String DATA_SOURCE_PROPERTIES = "legacyDataSourceProperties";

    @Bean(DATA_SOURCE_PROPERTIES)
    @ConfigurationProperties(prefix = "datasource.legacy")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(DATA_SOURCE)
    @ConfigurationProperties(prefix = "datasource.legacy.hikari")
    public DataSource dataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean(VENDOR_ADAPTER)
    public JpaVendorAdapter jpaVendorAdapter(@Qualifier(DATA_SOURCE) DataSource dataSource, JpaProperties jpaProperties) {
        AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setDatabase(jpaProperties.determineDatabase(dataSource));
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return adapter;
    }

    @Bean(ENTITY_MANAGER)
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier(VENDOR_ADAPTER) JpaVendorAdapter jpaVendorAdapter,
            ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
            JpaProperties jpaProperties,
            @Qualifier(DATA_SOURCE) DataSource dataSource
    ) {
        return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaProperties.getProperties(), persistenceUnitManager.getIfAvailable())
                .dataSource(dataSource)
                .packages(DOMAIN_PACKAGE)
                .build();
    }

    @Bean(TX_MANAGER)
    public PlatformTransactionManager legacyTxManager(@Qualifier(ENTITY_MANAGER)LocalContainerEntityManagerFactoryBean entityManager) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManager.getObject());
        return transactionManager;
    }
}
