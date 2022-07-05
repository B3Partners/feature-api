package nl.b3p.featureapi.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.naming.NamingException;
import javax.sql.DataSource;

@Configuration
@EntityScan(basePackages = {"nl.viewer.config"})
@EnableJpaRepositories(basePackages = "nl.b3p.featureapi.repository.fla",
        entityManagerFactoryRef = "viewerEntityManagerFactory",
        transactionManagerRef= "viewerTransactionManager"
)
public class JPAConfiguration {

    @Autowired
    private Environment env;

    @Primary
    @Bean(name="viewerEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean viewerEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setPersistenceUnitName("viewer-config-postgresql");
        em.setDataSource(viewerDataSource());
        return em;
    }

    @Bean
    public DataSource viewerDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driverClassName"));
        dataSource.setUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Primary
    @Bean
    public PlatformTransactionManager viewerTransactionManager() throws NamingException {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(viewerEntityManagerFactory().getObject());
        return transactionManager;
    }
}

