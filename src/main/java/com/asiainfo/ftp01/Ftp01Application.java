package com.asiainfo.ftp01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Ftp01Application {

    public static void main(String[] args) {
        SpringApplication.run(Ftp01Application.class, args);
    }

}

