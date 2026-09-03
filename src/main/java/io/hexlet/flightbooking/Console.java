package io.hexlet.flightbooking;

import java.sql.SQLException;

public final class Console {

    private Console() {}

    public static void main(String[] args) throws SQLException {
        var command = args.length == 0 ? "" : args[0];
        var dataSource = Database.dataSource();

        switch (command) {
            case "migrate" -> {
                var applied = new Migrator(dataSource).migrate();
                System.out.println(
                        applied.isEmpty()
                                ? "Миграции: применять нечего"
                                : "Миграции применены: " + String.join(", ", applied));
            }
            case "seed" -> {
                var inserted = new Seeder(dataSource).seed();
                System.out.println("Справочные данные залиты, новых рейсов: " + inserted);
            }
            default -> {
                System.err.println("Использование: console <migrate|seed>");
                System.exit(1);
            }
        }
    }
}
