package com.shopease.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Single shared connection pool for the whole app. A pool (rather than opening a
 * new java.sql.Connection per request) is what keeps product/search queries fast
 * under load - connections are reused instead of re-established every time.
 */
public final class DBConnectionManager {

    private static final HikariDataSource DATA_SOURCE;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env("DB_URL", "jdbc:mysql://localhost:3306/shopease?useSSL=false&serverTimezone=UTC"));
        config.setUsername(env("DB_USER", "root"));
        config.setPassword(env("DB_PASSWORD", ""));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(Integer.parseInt(env("DB_POOL_SIZE", "10")));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10_000);
        config.setPoolName("shopease-pool");
        DATA_SOURCE = new HikariDataSource(config);
    }

    private DBConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
