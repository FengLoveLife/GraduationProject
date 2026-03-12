package com.saul;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.saul.mapper")
public class SupermarketSalesForecastApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketSalesForecastApplication.class, args);
    }

}
