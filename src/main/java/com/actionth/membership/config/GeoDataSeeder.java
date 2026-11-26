package com.actionth.membership.config;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills the Thai province / district / subdistrict tables (postcode lookup in every address form)
 * from {@code db/geoThailand.sql} when they are empty or incomplete.
 *
 * <p>Production once ran with these tables empty, so typing a postcode offered no provinces at all
 * (tester deck #10), and a deploy that skipped the manual SQL step left it that way. Seeding at
 * startup makes the data arrive with the app. The script upserts by code, so running it over a
 * partly filled table is safe. Local dev ({@code ddl-auto: create}) gets the data on every boot too.
 *
 * <p>A failure is logged and never stops the app from starting.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeoDataSeeder implements CommandLineRunner {

    private static final String SCRIPT = "db/geoThailand.sql";
    /** The script has 7,436 subdistricts; fewer means the data is missing or was cut short. */
    private static final long EXPECTED_SUBDISTRICTS = 7000;

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Override
    public void run(String... args) {
        try {
            long subdistricts = countSubdistricts();
            if (subdistricts >= EXPECTED_SUBDISTRICTS) {
                return;
            }
            log.info("[GeoDataSeeder] geoSubdistricts has {} rows, loading {}", subdistricts, SCRIPT);
            String sql = new String(new ClassPathResource(SCRIPT).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8);
            // The JDBC connection already talks utf8mb4; SET NAMES is only for the mysql CLI.
            sql = sql.replaceAll("(?im)^\\s*SET\\s+NAMES[^;]*;", "");
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)),
                                StandardCharsets.UTF_8));
            }
            log.info("[GeoDataSeeder] done: {} provinces, {} subdistricts",
                    count("geoProvinces"), countSubdistricts());
        } catch (Exception e) {
            log.error("[GeoDataSeeder] could not load {}: {}", SCRIPT, e.getMessage(), e);
        }
    }

    private long countSubdistricts() {
        try {
            return count("geoSubdistricts");
        } catch (Exception missingTable) {
            return 0; // the script creates the tables
        }
    }

    private long count(String table) {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Long.class);
        return n == null ? 0 : n;
    }
}
