package org.sokybot.persistence.config;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.swing.Icon;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.sokybot.ICacheStorage;
//import org.sokybot.gameloader.GameLoader;
import org.sokybot.loader.IGameLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;



@ComponentScan(basePackages = { "org.sokybot.machinegroup" })
@EnableJpaRepositories(entityManagerFactoryRef = "gameEntityManagerFactory")
//@EnableJpaRepositories("org.sokybot.machinegroup.*")
//@EntityScan("org.sokybot.machinegroup.*")
//@SpringBootApplication
@Configuration
@EnableTransactionManagement
public class MachineGroupConfig {

	
	private static final String[] ENTITYMANAGER_PACKAGE = {"org.sokybot.machinegroup"} ; 
	
	@Value("${gamePath}")
	String gamePath;

	@Autowired
	ApplicationContext ctx;

	@Bean(destroyMethod = "close")
	@Profile({"test" , "dev" ,"prod"})
	public DataSource dataSource() {
		
		DataSourceBuilder builder = DataSourceBuilder.create(); 
		
		//final DriverManagerDataSource dataSource = new DriverManagerDataSource();
		builder.driverClassName("org.h2.Driver");
		builder.url("jdbc:h2:file:./" + this.gamePath.hashCode());
		builder.username("sa");
		builder.password("password");

		return builder.build();
	}


	 @Bean("transactionManager")
	 @Primary
	@Profile({"test" , "dev" ,"prod"})
	 public JpaTransactionManager jpaTransactionManager() {
         JpaTransactionManager transactionManager = new JpaTransactionManager();
         
         transactionManager.setEntityManagerFactory(gameEntityManagerFactory().getObject());
         return transactionManager;
     }
     
	@Bean
	@Profile({"test" , "dev" ,"prod"})
	public LocalContainerEntityManagerFactoryBean gameEntityManagerFactory() {
		LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
		bean.setJpaVendorAdapter(hibernateJpaVendorAdapter());
		bean.setDataSource(dataSource());
		bean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
		bean.setPackagesToScan(ENTITYMANAGER_PACKAGE);
		bean.setJpaProperties(hibernateProperties()) ;
		bean.afterPropertiesSet();
		return bean;
	}

	private Properties hibernateProperties() { 
		Properties prop = new Properties() ; 
	
		prop.setProperty("hibernate.hbm2ddl.auto", "update");
		prop.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect") ;

		return prop ; 
		
	}
	private HibernateJpaVendorAdapter hibernateJpaVendorAdapter() {
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();

		adapter.setShowSql(false);
		return adapter;
	}

	@Bean
	@Profile({"test" , "dev" ,"prod"})
	Icon gameIcon() {

		// here we must load game icon from game path and resize it to
		// this statement for test
		// use FlatSVGIcon


		return null;

	}

	// Removed commented out IEntityExtractorFactory bean

	@Bean
	@Profile({"test" , "dev" ,"prod"})
	IGameLoader gameLoader() {
		// now we depend on specific implementation
		// but we must get a target instance from GameLoaderRegistry service
		// according to user configuration
		return null;
	}

	public static void main(String args[]) {

		String path = "E:\\Amroo\\Silkroad Games\\LegionSRO_15_08_2019";
		System.out.println("Path Hash Code : " + path.hashCode());
		System.out.println("Path Hash Code : " + path.hashCode());
		System.out.println("Path Hash Code : " + path.hashCode());

	}

}
