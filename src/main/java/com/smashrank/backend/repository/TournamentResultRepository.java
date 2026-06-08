package com.smashrank.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smashrank.backend.model.TournamentResult;

import java.util.List;
import java.util.Optional;


@Repository
public interface TournamentResultRepository extends JpaRepository<TournamentResult, Long> {

    List<TournamentResult> findByPlayerId(Long playerId);

    List<TournamentResult> findByStartGgTournamentId(String startGgTournamentId);

    Optional<TournamentResult> findByPlayerIdAndStartGgTournamentId(Long playerId, String startGgTournamentId);

    boolean existsByPlayerIdAndStartGgTournamentId(Long playerId, String startGgTournamentId);

    /** Resultados de un jugador ordenados por fecha descendente */
    @Query("SELECT tr FROM TournamentResult tr WHERE tr.player.id = :playerId ORDER BY tr.tournamentDate DESC")
    List<TournamentResult> findByPlayerIdOrderByDateDesc(@Param("playerId") Long playerId);

    /** Suma total de puntos de un jugador */
    @Query("SELECT COALESCE(SUM(tr.pointsEarned), 0) FROM TournamentResult tr WHERE tr.player.id = :playerId")
    Integer sumPointsByPlayerId(@Param("playerId") Long playerId);
}
