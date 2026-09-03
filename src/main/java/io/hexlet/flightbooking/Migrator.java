package io.hexlet.flightbooking;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.sql.DataSource;

public final class Migrator {

    private static final String MIGRATIONS_LIST = "db/migrations.txt";

    private final DataSource dataSource;

    public Migrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<String> migrate() throws SQLException {
        try (var connection = Database.connectWithRetries(dataSource);
                var statement = connection.createStatement()) {
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        name       text PRIMARY KEY,
                        applied_at timestamptz NOT NULL DEFAULT now()
                    )
                    """);

            var applied = new HashSet<String>();
            try (var rows = statement.executeQuery("SELECT name FROM schema_migrations")) {
                while (rows.next()) {
                    applied.add(rows.getString("name"));
                }
            }

            var done = new ArrayList<String>();
            for (var name : migrationNames()) {
                if (applied.contains(name)) {
                    continue;
                }

                apply(connection, name);
                done.add(name);
            }

            return done;
        }
    }

    private void apply(java.sql.Connection connection, String name) throws SQLException {
        var sql = readResource("db/migrations/" + name);

        // Миграция и запись о ней — одна транзакция: иначе половина применённой схемы останется без
        // отметки, и второй запуск упадёт на уже созданных объектах.
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement();
                var insert =
                        connection.prepareStatement(
                                "INSERT INTO schema_migrations (name) VALUES (?)")) {
            statement.execute(sql);
            insert.setString(1, name);
            insert.executeUpdate();
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();

            throw error;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private List<String> migrationNames() {
        return readResource(MIGRATIONS_LIST)
                .lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .sorted()
                .toList();
    }

    private String readResource(String path) {
        try (var stream = Migrator.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Не найден ресурс " + path);
            }

            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
