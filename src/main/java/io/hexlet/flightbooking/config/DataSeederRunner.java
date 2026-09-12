package io.hexlet.flightbooking.config;

import io.hexlet.flightbooking.seed.Seeder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeederRunner implements ApplicationRunner {

    private final Seeder seeder;

    public DataSeederRunner(Seeder seeder) {
        this.seeder = seeder;
    }

    @Override
    public void run(ApplicationArguments args) {
        var inserted = seeder.seed();
        System.out.println("Справочники залиты, новых рейсов: " + inserted);
    }
}
