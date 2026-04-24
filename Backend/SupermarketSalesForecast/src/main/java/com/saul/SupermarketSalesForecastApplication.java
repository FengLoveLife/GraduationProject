package com.saul;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan({"com.saul.user.mapper", "com.saul.user.log.mapper", "com.saul.product.mapper",
             "com.saul.inventory.mapper", "com.saul.sales.mapper", "com.saul.upload.mapper",
             "com.saul.forecast.mapper", "com.saul.restocking.mapper",
             "com.saul.alert.mapper"})
public class SupermarketSalesForecastApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketSalesForecastApplication.class, args);
    }

}
