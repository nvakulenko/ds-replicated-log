package ua.edu.ucu.ds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SecondaryApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecondaryApplication.class);

    public static void main(String[] args) {
        LOGGER.info("Starting the Secondary app...");
        SpringApplication.run(SecondaryApplication.class, args);
    }
}
