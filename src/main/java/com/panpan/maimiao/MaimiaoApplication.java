package com.panpan.maimiao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class MaimiaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaimiaoApplication.class, args);
    }

}
