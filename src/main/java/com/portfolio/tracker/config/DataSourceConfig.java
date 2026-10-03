package com.portfolio.tracker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Explicit DataSource configuration using HikariCP.
 *
 * Spring Boot auto-configures HikariCP from application.properties by default.
 * We override here to show interviewers we understand what Spring Boot does
 * under the hood — and to add custom pool configuration programmatically.
 *
 * NOTE: With this class present, the spring.datasource.* properties in
 * application.properties are still honoured because we read them with @Value.
 */
@Configuration
public class DataSourceConfig {

    // Read connection details from application.properties
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:30000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    /**
     * Constructs and returns a configured HikariDataSource.
     *
     * HikariCP is the fastest JDBC connection pool available.
     * It maintains a pool of pre-opened connections, so each request
     * doesn't pay the cost of opening a new TCP connection to MySQL.
     */
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        // MySQL-specific property: reduce latency by caching prepared statements
        config.addDataSourceProperty("cachePrepStmts",          "true");
        config.addDataSourceProperty("prepStmtCacheSize",        "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setPoolName("PortfolioHikariPool");

        // Validates connections are alive before handing them to callers
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }
}
