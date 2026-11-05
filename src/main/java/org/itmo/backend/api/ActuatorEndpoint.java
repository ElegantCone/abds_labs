package org.itmo.backend.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ActuatorEndpoint {
    @GetMapping(value = "/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok().build();
    }
}
