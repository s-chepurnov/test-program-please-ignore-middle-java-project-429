package io.hexlet.flightbooking;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import javax.sql.DataSource;

public final class Database {

    private static final int CONNECT_ATTEMPTS = 30;
    private static final long CONNECT_DELAY_MS = 500;
    private static final int DEFAULT_PORT = 5432;

    private Database() {}

    public record Settings(String jdbcUrl, String username, String password) {}

    public static Settings parse(String databaseUrl) {
        Objects.requireNonNull(databaseUrl, "Не задана переменная окружения DATABASE_URL");
        if (databaseUrl.isBlank()) {
            throw new IllegalArgumentException("Не задана переменная окружения DATABASE_URL");
        }

        var uri = URI.create(databaseUrl);
        var host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Не удалось разобрать DATABASE_URL: " + databaseUrl);
        }

        // Порт в адресе может отсутствовать — тогда берём стандартный для PostgreSQL.
        var port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();
        var path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");

        // Параметры запроса (sslmode и прочее) переносим как есть: они одинаково понятны драйверу.
        var query = uri.getQuery() == null ? "" : "?" + uri.getQuery();
        var jdbcUrl = "jdbc:postgresql://%s:%d/%s%s".formatted(host, port, path, query);

        // Логин и пароль в адресе необязательны: у локальной базы без пароля их не будет.
        var userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isEmpty()) {
            return new Settings(jdbcUrl, null, null);
        }

        var parts = userInfo.split(":", 2);

        return new Settings(jdbcUrl, parts[0], parts.length > 1 ? parts[1] : null);
    }

    public static String databaseUrl() {
        var url = System.getenv("DATABASE_URL");
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Не задана переменная окружения DATABASE_URL");
        }

        return url;
    }

    public static DataSource dataSource() {
        return dataSource(databaseUrl());
    }

    public static DataSource dataSource(String databaseUrl) {
        var settings = parse(databaseUrl);
        var config = new HikariConfig();

        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(4);
        config.setConnectionInitSql("SET TIME ZONE 'UTC'");

        return new HikariDataSource(config);
    }

    public static Connection connectWithRetries(DataSource dataSource) throws SQLException {
        SQLException lastError = null;

        for (var attempt = 0; attempt < CONNECT_ATTEMPTS; attempt++) {
            try {
                return dataSource.getConnection();
            } catch (SQLException error) {
                lastError = error;
                try {
                    Thread.sleep(CONNECT_DELAY_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();

                    throw error;
                }
            }
        }

        throw new SQLException("Не удалось подключиться к базе", lastError);
    }
}
