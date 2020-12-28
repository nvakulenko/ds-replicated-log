package ua.edu.ucu.ds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MasterApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MasterApplication.class);

    public static void main(String[] args) {
        LOGGER.info("Starting the Master app...");
        SpringApplication.run(MasterApplication.class, args);
    }

}
