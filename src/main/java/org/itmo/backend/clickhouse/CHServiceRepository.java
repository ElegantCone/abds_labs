package org.itmo.backend.clickhouse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.itmo.backend.dto.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CHServiceRepository {
    private final JdbcClient jdbcClient;

    public CHServiceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<User> findUserById(long id) {
        return jdbcClient.sql("SELECT * FROM dim_users FINAL WHERE id = :id LIMIT 1")
                .param("id", id)
                .query((rs, _) -> User.fromResultSet(rs))
                .optional();
    }

    public synchronized void updateUserById(User user, UserPatch patch) {
        String phone = patch.phone() == null ? user.phone() : patch.phone();
        String previousPhone = phone.equals(user.phone()) ? user.phone_prev() : user.phone();

        jdbcClient.sql("""
                        ALTER TABLE dim_users UPDATE
                            fio = :fio,
                            phone = :phone,
                            phone_prev = :previousPhone
                        WHERE id = :id
                        SETTINGS mutations_sync = 1
                        """)
                .param("fio", patch.fio() == null ? user.fio() : patch.fio())
                .param("phone", phone)
                .param("previousPhone", previousPhone)
                .param("id", user.id())
                .update();
    }

    public Optional<Realty> findRealtyById(long id) {
        return jdbcClient.sql("SELECT * FROM dim_realty WHERE id = :id LIMIT 1")
                .param("id", id)
                .query((rs, _) -> Realty.fromResultSet(rs))
                .optional();
    }

    public Optional<Tariff> findTariffById(long id) {
        return jdbcClient.sql("SELECT * FROM dim_tariffs FINAL WHERE id = :id LIMIT 1")
                .param("id", id)
                .query((rs, _) -> Tariff.fromResultSet(rs))
                .optional();
    }

    public Optional<Gauge> findGaugeById(long id) {
        return jdbcClient.sql("SELECT * FROM dim_gauges WHERE id = :id LIMIT 1")
                .param("id", id)
                .query((rs, _) -> Gauge.fromResultSet(rs))
                .optional();
    }

    public Optional<Tariff> findTariffByDate(Long id, LocalDate date) {
        return jdbcClient.sql("""
                        SELECT id, name, price_day_kwph, price_night_kwph, valid_from, valid_to
                        FROM dim_tariffs FINAL
                        WHERE (:any_tariff OR id = :id)
                          AND valid_from <= toDateTime(:date) AND toDateTime(:date) < valid_to
                        ORDER BY id ASC
                        LIMIT 1
                        """)
                .param("date", date.toString())
                .param("id", id == null ? 0 : id)
                .param("any_tariff", id == null)
                .query((rs, _) -> Tariff.fromResultSet(rs))
                .optional();
    }

    public synchronized boolean saveReading(long id, Reading reading) {
        if (findRealtyById(id).isEmpty()
                || findUserById(reading.userId()).isEmpty()
                || findTariffById(reading.tariffId()).isEmpty()
                || findGaugeById(reading.gaugeId()).isEmpty()) {
            return false;
        }
        jdbcClient.sql(
                        """
                                INSERT INTO fact_meter_readings
                                    (realty_id, user_id, tariff_id, gauge_id, reading_date, day_kw, night_kw, created_at)
                                SELECT :realty_id, :user_id, :tariff_id, :gauge_id, :reading_date, :day_kw, :night_kw,
                                       greatest(now(), max(created_at) + INTERVAL 1 SECOND)
                                FROM fact_meter_readings
                                WHERE realty_id = :realty_id AND gauge_id = :gauge_id AND reading_date = :reading_date
                                """)
                .param("realty_id", id)
                .param("user_id", reading.userId())
                .param("tariff_id", reading.tariffId())
                .param("gauge_id", reading.gaugeId())
                .param("reading_date", reading.readingDate())
                .param("day_kw", reading.dayKw())
                .param("night_kw", reading.nightKw())
                .update();
        return true;
    }

    public List<Reading> findReadingsByRealty(long id, LocalDate dateFrom, LocalDate dateTo) {
        return jdbcClient.sql("""
                        SELECT reading_date, user_id, tariff_id, gauge_id, day_kw, night_kw
                        FROM fact_meter_readings FINAL
                        WHERE realty_id = :id AND reading_date >= :date_from AND reading_date <= :date_to
                        ORDER BY reading_date ASC, created_at DESC, gauge_id DESC
                        LIMIT 1 BY reading_date
                        """)
                .param("id", id)
                .param("date_from", dateFrom)
                .param("date_to", dateTo)
                .query((rs, _) -> Reading.fromResultSet(rs))
                .list();
    }

    public List<Invoice> findInvoicesByRealty(long id, long limit, long offset) {
        return jdbcClient.sql("""
                        SELECT tariff_id, gauge_id, day_kw, night_kw, price, deadline, created_at
                        FROM fact_invoice
                        WHERE realty_id = :id
                        ORDER BY created_at DESC, gauge_id DESC, tariff_id DESC, user_id DESC,
                                 deadline DESC, price DESC, day_kw DESC, night_kw DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("id", id)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, _) -> Invoice.fromResultSet(rs))
                .list();
    }

    public long countInvoicesByRealty(long id) {
        return jdbcClient.sql("SELECT count() FROM fact_invoice WHERE realty_id = :id")
                .param("id", id)
                .query(Long.class)
                .single();
    }

    public synchronized boolean installGauge(long realtyId, long gaugeId) {
        if (findRealtyById(realtyId).isEmpty() || findGaugeById(gaugeId).isEmpty()) return false;
        var active = jdbcClient.sql("""
                        SELECT gauge_id, created_at, version
                        FROM fact_gauge_bindings FINAL
                        WHERE realty_id = :realty_id AND sign = 1
                        ORDER BY version DESC, created_at DESC, gauge_id DESC
                        LIMIT 1
                        """)
                .param("realty_id", realtyId)
                .query((rs, _) -> new GaugeBinding(rs.getLong("gauge_id"),
                        rs.getString("created_at"), rs.getLong("version")))
                .optional();

        if (active.isEmpty()) {
            jdbcClient.sql("""
                            INSERT INTO fact_gauge_bindings
                                (gauge_id, realty_id, created_at, sign, version)
                            VALUES (:gauge_id, :realty_id, now(), 1, 1)
                            """)
                    .param("gauge_id", gaugeId)
                    .param("realty_id", realtyId)
                    .update();
            return true;
        }

        var previous = active.get();
        jdbcClient.sql("""
                        INSERT INTO fact_gauge_bindings
                            (gauge_id, realty_id, created_at, sign, version)
                        VALUES (:previous_gauge_id, :realty_id, :previous_created_at, -1, :previous_version),
                               (:gauge_id, :realty_id, now(), 1, :version)
                        """)
                .param("previous_gauge_id", previous.gaugeId())
                .param("realty_id", realtyId)
                .param("previous_created_at", previous.createdAt())
                .param("previous_version", previous.version())
                .param("gauge_id", gaugeId)
                .param("version", Math.incrementExact(previous.version()))
                .update();
        return true;
    }
}
