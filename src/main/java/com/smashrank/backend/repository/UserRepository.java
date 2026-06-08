package com.smashrank.backend.repository;

import com.smashrank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para realizar operaciones sobre la entidad User.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario registrado por su ID único de Start.gg.
     */
    Optional<User> findByStartGgUserId(String startGgUserId);
}
