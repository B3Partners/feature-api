package nl.b3p.featureapi.configuration;

import nl.b3p.featureapi.resource.gbi.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "nl.b3p.featureapi.repository.gbi",
        entityManagerFactoryRef = "gbiEntityManagerFactory",
        transactionManagerRef= "gbiTransactionManager"
)
public class GBIConfiguration {
    @Autowired
    private Environment env;

    @Bean(name="gbiEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean gbiEntityManagerFactory(EntityManagerFactoryBuilder builder) {

        return builder
                .dataSource(gbiDataSource())
                .packages(Collection.class)
                .build();
    }

    @Bean
    @Primary
    public DataSource gbiDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.datasource2.driverClassName"));
        dataSource.setUrl(env.getProperty("spring.datasource2.url"));
        dataSource.setUsername(env.getProperty("spring.datasource2.username"));
        dataSource.setPassword(env.getProperty("spring.datasource2.password"));
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager gbiTransactionManager(
            final @Qualifier("gbiEntityManagerFactory") LocalContainerEntityManagerFactoryBean cardEntityManagerFactory
    ) {
        return new JpaTransactionManager(cardEntityManagerFactory.getObject());
    }
}
