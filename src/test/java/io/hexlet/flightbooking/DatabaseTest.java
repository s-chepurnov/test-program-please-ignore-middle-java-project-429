package io.hexlet.flightbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Разбор {@code DATABASE_URL} — единственный кусок решения, который живёт вне HTTP, и единственный,
 * где ошибка видна не как «контракт нарушен», а как «приложение не стартует». Поэтому у него свои
 * тесты: если API-тесты падают все сразу, эти отделяют «не разобрали адрес» от «сломали контракт».
 */
class DatabaseTest {

    @Test
    void разбираетАдресСЛогиномПаролемИПортом() {
        var settings = Database.parse("postgres://user:secret@db:5432/appdb");

        assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/appdb");
        assertThat(settings.username()).isEqualTo("user");
        assertThat(settings.password()).isEqualTo("secret");
    }

    @Test
    void подставляетСтандартныйПортЕслиЕгоНетВАдресе() {
        var settings = Database.parse("postgres://user:secret@db/appdb");

        assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/appdb");
    }

    @Test
    void переноситПараметрыЗапроса() {
        var settings = Database.parse("postgres://user:secret@db:5432/appdb?sslmode=require");

        assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://db:5432/appdb?sslmode=require");
    }

    @Test
    void работаетБезЛогинаИПароля() {
        var settings = Database.parse("postgres://localhost:5432/appdb");

        assertThat(settings.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/appdb");
        assertThat(settings.username()).isNull();
        assertThat(settings.password()).isNull();
    }

    @Test
    void пустойАдресЭтоОшибкаНастройки() {
        assertThatThrownBy(() -> Database.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DATABASE_URL");
    }
}
