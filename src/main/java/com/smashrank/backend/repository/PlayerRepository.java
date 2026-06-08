package com.smashrank.backend.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smashrank.backend.model.Player;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Player.
 * Extiende JpaRepository para CRUD completo + paginación.
 */
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByGamertag(String gamertag);

    Optional<Player> findByStartGgUserId(String startGgUserId);

    boolean existsByGamertag(String gamertag);

    boolean existsByStartGgUserId(String startGgUserId);

    List<Player> findByCountryIgnoreCase(String country);

    /** Obtiene el ranking ordenado por puntos descendente */
    @Query("SELECT p FROM Player p ORDER BY p.rankPoints DESC")
    List<Player> findAllOrderByRankPointsDesc();

    /** Top N jugadores del ranking (default 15) */
    @Query("SELECT p FROM Player p ORDER BY p.rankPoints DESC")
    List<Player> findTopPlayers(Pageable pageable);

    /** Busca jugadores por gamertag (búsqueda parcial, case-insensitive) */
    @Query("SELECT p FROM Player p WHERE LOWER(p.gamertag) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Player> searchByGamertag(@Param("query") String query);
}
