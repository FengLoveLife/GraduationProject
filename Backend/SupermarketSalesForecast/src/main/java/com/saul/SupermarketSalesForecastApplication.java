package com.saul;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.saul.user.mapper", "com.saul.user.log.mapper", "com.saul.product.mapper",
             "com.saul.inventory.mapper", "com.saul.sales.mapper", "com.saul.upload.mapper",
             "com.saul.forecast.mapper", "com.saul.restocking.mapper"})
public class SupermarketSalesForecastApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupermarketSalesForecastApplication.class, args);
    }

}
