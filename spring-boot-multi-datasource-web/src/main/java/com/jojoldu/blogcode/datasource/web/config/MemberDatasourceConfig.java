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

import static com.jojoldu.blogcode.datasource.web.config.MemberDatasourceConfig.DOMAIN_PACKAGE;
import static com.jojoldu.blogcode.datasource.web.config.MemberDatasourceConfig.ENTITY_MANAGER;
import static com.jojoldu.blogcode.datasource.web.config.MemberDatasourceConfig.TX_MANAGER;

/**
 * JPA를 분리해서 쓰기 위해선 3개가 필요
 * Datasource
 * EntityManager
 * TransactionManager
 */
@Configuration
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(basePackages = DOMAIN_PACKAGE, entityManagerFactoryRef = ENTITY_MANAGER, transactionManagerRef = TX_MANAGER)
public class MemberDatasourceConfig {

    public static final String DOMAIN_PACKAGE = "com.jojoldu.blogcode.datasource.core.member";
    public static final String ENTITY_MANAGER = "memberEntityManager";
    public static final String TX_MANAGER = "memberTxManager";
    public static final String DATA_SOURCE = "memberDataSource";
    public static final String VENDOR_ADAPTER = "memberJpaVendorAdapter";
    public static final String ENTITY_FACTORY_BUILDER = "memberJpaEntityFactoryBuilder";
    public static final String DATA_SOURCE_PROPERTIES = "memberDataSourceProperties";

    @Bean(DATA_SOURCE_PROPERTIES)
    @ConfigurationProperties(prefix = "datasource.jpa")
    public DataSourceProperties orderDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = DATA_SOURCE)
    @ConfigurationProperties(prefix = "datasource.jpa.hikari")
    public DataSource jpaDataSource() {
        return orderDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = TX_MANAGER)
    public PlatformTransactionManager jpaSessionTxManager() {
        return new JpaTransactionManager();
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

    @Bean(ENTITY_FACTORY_BUILDER)
    @Primary
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            @Qualifier(VENDOR_ADAPTER) JpaVendorAdapter jpaVendorAdapter,
            ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
            JpaProperties jpaProperties
    ) {
        return new EntityManagerFactoryBuilder(
                jpaVendorAdapter, jpaProperties.getProperties(),
                persistenceUnitManager.getIfAvailable());
    }

    @Bean(ENTITY_MANAGER)
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier(ENTITY_FACTORY_BUILDER) EntityManagerFactoryBuilder factoryBuilder,
            @Qualifier(DATA_SOURCE) DataSource dataSource,
            JpaProperties jpaProperties
    ) {
        return factoryBuilder
                .dataSource(dataSource)
                .packages(DOMAIN_PACKAGE)
                .properties(jpaProperties.getHibernateProperties(new HibernateSettings().ddlAuto(() -> {
                    if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
                        return "none";
                    }
                    return "create-drop";
                })))
                .build();
    }
}
