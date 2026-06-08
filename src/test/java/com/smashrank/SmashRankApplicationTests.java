package com.smashrank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.smashrank.backend.service.PlayerService;

@SpringBootTest
@ActiveProfiles("test")
class SmashRankApplicationTests {

    @Autowired
    private PlayerService playerService;

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring se carga correctamente
    }
}
