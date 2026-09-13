package io.hexlet.flightbooking.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${DATABASE_URL:postgres://postgres:postgres@localhost:5432/flight-booking}")
            String rawUrl) {

        try {
            var parsed = parseDatabaseUrl(rawUrl);

            var config = new HikariConfig();
            config.setJdbcUrl(parsed.jdbcUrl());
            config.setUsername(parsed.username());
            config.setPassword(parsed.password());

            return new HikariDataSource(config);
        } catch(Exception e) {
            log.error("DATABASE_URL = {}", rawUrl);

            throw new IllegalStateException("Не удалось настроить DataSource: " + e.getMessage(), e);
        }
    }

    record ParsedUrl(String jdbcUrl, String username, String password) {}

    static ParsedUrl parseDatabaseUrl(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is empty");
        }
        if (raw.startsWith("jdbc:")) {
            throw new IllegalArgumentException(
                    "DATABASE_URL must be in Render-style postgres://... (), got JDBC URL: " + raw);
        }

        var uri = URI.create(raw);
        var host = uri.getHost();
        var port = uri.getPort() == -1 ? 5432 : uri.getPort();
        var path = uri.getPath();
        var database = path == null ? "" : path.replaceFirst("^/", "");

        var username = "postgres";
        var password = "postgres";
        var userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            var parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        var jdbcUrl = "jdbc:postgresql://%s:%d/%s".formatted(host, port, database);
        return new ParsedUrl(jdbcUrl, username, password);
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
