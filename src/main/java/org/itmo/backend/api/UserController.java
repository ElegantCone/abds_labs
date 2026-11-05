package org.itmo.backend.api;

import org.itmo.backend.dto.UserPatch;
import org.itmo.backend.clickhouse.CHServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

import static org.itmo.backend.utils.ExceptionUtils.found;
import static org.itmo.backend.utils.ExceptionUtils.notFound;

@RestController
@RequestMapping("/users")
public class UserController {
    private final CHServiceRepository repository;

    public UserController(CHServiceRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable("id") long id) {
        return found(repository.findUserById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patchUser(@PathVariable("id") long id,
                                       @RequestBody Map<String, String> fields) {
        UserPatch patch = UserPatch.from(fields);
        var user = repository.findUserById(id);
        if (user.isEmpty()) return notFound();
        repository.updateUserById(user.get(), patch);
        return ResponseEntity.noContent().build();
    }
}
