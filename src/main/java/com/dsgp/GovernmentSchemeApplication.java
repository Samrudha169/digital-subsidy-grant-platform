package com.dsgp;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GovernmentSchemeApplication {

    public static void main(String[] args) {

        // Load .env from the project root (if present) and push every variable
        // into System properties so that Spring's ${VAR:default} placeholders
        // resolve automatically — without any IntelliJ run-configuration setup.
        // .env is git-ignored; production environments supply vars via the OS.
        Dotenv dotenv = Dotenv.configure()
                .directory("./")          // project root where .env lives
                .ignoreIfMissing()        // no-op in CI / production (env vars already set)
                .systemProperties()       // expose as System.getProperty() & Spring env
                .load();

        SpringApplication.run(GovernmentSchemeApplication.class, args);
    }
}