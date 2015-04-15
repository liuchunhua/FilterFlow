package com.lch;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/*
  --remote debugging
  java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=n \
       -jar target/myproject-0.0.1-SNAPSHOT.jar

  export MAVEN_OPTS=-Xmx1024m -XX:MaxPermSize=128M -Djava.security.egd=file:/dev/./urandom
  mvn spring-boot:run
 */

//@Import //import additional configuration class
//@ImportResouce //load XML configuration files
@ImportResource("classpath:/context.xml")
@Configuration
@ComponentScan
/*
  Spring Boot auto-configuration attempts to automatically configure your Spring application based on the jar dependencies that you have added
 */
@EnableAutoConfiguration
//@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
public class Application {

    public static void main(String[] args) {
        //SpringApplication.run(Application.class, args);
		SpringApplication app = new SpringApplication(Application.class);
		app.setShowBanner(false);
		//app.setWebEnvironment(true);
		app.run(args);
    }
}
