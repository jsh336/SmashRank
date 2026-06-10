package com.smashrank.backend.controller;

import com.smashrank.backend.model.SavedRanking;
import com.smashrank.backend.repository.SavedRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/saved-rankings")
@RequiredArgsConstructor
public class SavedRankingController {

    private final SavedRankingRepository savedRankingRepository;

    @GetMapping
    public ResponseEntity<List<SavedRanking>> getAllSavedRankings() {
        log.info("GET /api/v1/saved-rankings - Obteniendo todos los rankings guardados");
        return ResponseEntity.ok(savedRankingRepository.findAllByOrderByCalculatedAtDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavedRanking> getSavedRankingById(@PathVariable Long id) {
        log.info("GET /api/v1/saved-rankings/{} - Obteniendo ranking guardado", id);
        return savedRankingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SavedRanking> saveRanking(@RequestBody SavedRanking savedRanking) {
        log.info("POST /api/v1/saved-rankings - Guardando nuevo ranking regional: {} - {}", savedRanking.getRegion(), savedRanking.getName());
        savedRanking.setCalculatedAt(LocalDateTime.now());
        return ResponseEntity.ok(savedRankingRepository.save(savedRanking));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavedRanking> updateRanking(@PathVariable Long id, @RequestBody SavedRanking updatedRanking) {
        log.info("PUT /api/v1/saved-rankings/{} - Actualizando ranking guardado", id);
        return savedRankingRepository.findById(id)
                .map(existing -> {
                    existing.setName(updatedRanking.getName());
                    existing.setRankingData(updatedRanking.getRankingData());
                    existing.setTotalPlayers(updatedRanking.getTotalPlayers());
                    return ResponseEntity.ok(savedRankingRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRanking(@PathVariable Long id) {
        log.info("DELETE /api/v1/saved-rankings/{} - Eliminando ranking guardado", id);
        if (savedRankingRepository.existsById(id)) {
            savedRankingRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
