package org.itmo.backend.api;

import org.itmo.backend.clickhouse.CHServiceRepository;
import org.itmo.backend.dto.Reading;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static org.itmo.backend.utils.ExceptionUtils.*;

@RestController
@RequestMapping("/realty")
public class RealtyController {
    private final CHServiceRepository chServiceRepository;

    public RealtyController(CHServiceRepository chServiceRepository) {
        this.chServiceRepository = chServiceRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable("id") Long id) {
        return found(chServiceRepository.findRealtyById(id));
    }

    @PostMapping("/{id}/readings")
    public ResponseEntity<?> readings(@PathVariable("id") Long id, @RequestBody Map<String, Object> fields) {
        Reading reading = Reading.fromMap(fields);
        var saved = chServiceRepository.saveReading(id, reading);
        if (!saved) return notFound();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/readings")
    public ResponseEntity<?> getReadings(@PathVariable("id") long id,
                                        @RequestParam Map<String, String> parameters) {
        var dateFrom = date(parameters.get("date_from"), "date_from");
        var dateTo = date(parameters.get("date_to"), "date_to");
        if (dateFrom.isAfter(dateTo)) throw invalidParameter("date_from");
        if (chServiceRepository.findRealtyById(id).isEmpty()) return notFound();
        var readings = chServiceRepository.findReadingsByRealty(id, dateFrom, dateTo);
        return ResponseEntity.ok(Map.of(
                "readings", readings,
                "count", readings.size()
        ));
    }

    @GetMapping("/{id}/invoices")
    public ResponseEntity<?> getInvoices(@PathVariable("id") long id,
                                        @RequestParam Map<String, String> parameters) {
        long limit = unsignedParameter(parameters, "limit", 10);
        long offset = unsignedParameter(parameters, "offset", 0);
        if (chServiceRepository.findRealtyById(id).isEmpty()) return notFound();
        return ResponseEntity.ok(Map.of(
                "invoices", chServiceRepository.findInvoicesByRealty(id, limit, offset),
                "count", chServiceRepository.countInvoicesByRealty(id)
        ));
    }

    @PostMapping("/{id}/gauges")
    public ResponseEntity<?> installGauge(@PathVariable("id") long id,
                                         @RequestBody Map<String, Object> fields) {
        long gaugeId = parseUnsigned(fields, "gauge_id");
        var installed = chServiceRepository.installGauge(id, gaugeId);
        if (!installed) return notFound();
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
