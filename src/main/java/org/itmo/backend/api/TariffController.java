package org.itmo.backend.api;

import java.util.Map;

import org.itmo.backend.clickhouse.CHServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.itmo.backend.utils.ExceptionUtils.*;

@RestController
@RequestMapping("/tariffs")
public class TariffController {
    private final CHServiceRepository repository;

    public TariffController(CHServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<?> getTariff(@RequestParam Map<String, String> parameters) {
        var date = date(parameters.get("date"), "date");
        Long id = parameters.containsKey("id") ? unsignedParameter(parameters, "id", 0) : null;
        return found(repository.findTariffByDate(id, date));
    }
}
