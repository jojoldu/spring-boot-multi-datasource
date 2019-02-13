package com.jojoldu.blogcode.datasource.web.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA를 분리해서 쓰기 위해선 3개가 필요
 * Datasource
 * EntityManager
 * TransactionManager
 */
@Configuration
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(basePackages = "com.jojoldu.blogcode.datasource.core", entityManagerFactoryRef = , transactionManagerRef = )
public class DatasourceConfig {
    @Bean(CORE_DATA_SOURCE_PROPERTIES)
    @ConfigurationProperties(prefix = "datasource.jpa")
    public DataSourceProperties orderDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = JPA_DATA_SOURCE)
    @ConfigurationProperties(prefix = "datasource.jpa.hikari")
    public DataSource jpaDataSource() {
        return orderDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Primary
    @Bean(name = CoreJpaConfig.TX_MANAGER)
    public PlatformTransactionManager jpaSessionTxManager() {
        return new JpaTransactionManager();
    }

    @Bean(JPA_VENDOR_ADAPTER)
    public JpaVendorAdapter jpaVendorAdapter(@Qualifier(JPA_DATA_SOURCE) DataSource dataSource, JpaProperties jpaProperties) {
        AbstractJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setDatabase(jpaProperties.determineDatabase(dataSource));
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        return adapter;
    }

    @Bean(JPA_ENTITY_FACTORY_BUILDER)
    @Primary
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            @Qualifier(JPA_VENDOR_ADAPTER) JpaVendorAdapter jpaVendorAdapter,
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
            @Qualifier(JPA_ENTITY_FACTORY_BUILDER) EntityManagerFactoryBuilder factoryBuilder,
            @Qualifier(JPA_DATA_SOURCE) DataSource dataSource,
            JpaProperties jpaProperties
    ) {
        return factoryBuilder
                .dataSource(dataSource)
                .packages(JPA_DOMAIN_PACKAGE)
                .properties(jpaProperties.getHibernateProperties(new HibernateSettings().ddlAuto(() -> {
                    if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
                        return "none";
                    }
                    return "create-drop";
                })))
                .build();
    }
}
