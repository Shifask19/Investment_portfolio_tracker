package com.portfolio.tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Investment Portfolio Tracker application.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   - @Configuration   : marks this as a source of bean definitions
 *   - @EnableAutoConfiguration : lets Spring Boot auto-configure beans
 *   - @ComponentScan   : scans this package and sub-packages for components
 */
@SpringBootApplication
public class InvestmentPortfolioTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentPortfolioTrackerApplication.class, args);
    }
}
