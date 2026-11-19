package com.elvero.shop;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopApplication {

    public static void main(String[] args) {
        // The Node app stored every DATETIME in UTC (Sequelize default). Keeping the JVM in UTC means
        // LocalDateTime.now(), @CreationTimestamp and the database all agree, wherever the server runs.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ShopApplication.class, args);
    }
}
