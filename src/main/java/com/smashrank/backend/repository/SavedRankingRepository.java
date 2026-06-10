package com.smashrank.backend.repository;

import com.smashrank.backend.model.SavedRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SavedRankingRepository extends JpaRepository<SavedRanking, Long> {
    List<SavedRanking> findByRegionOrderByCalculatedAtDesc(String region);
    List<SavedRanking> findAllByOrderByCalculatedAtDesc();
}
