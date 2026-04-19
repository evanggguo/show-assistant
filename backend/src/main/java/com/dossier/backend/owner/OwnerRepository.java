package com.dossier.backend.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TDD 5.1 — Owner data access layer
 * Provides CRUD operations for the Owner entity
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    java.util.Optional<Owner> findByUsername(String username);

    boolean existsByUsername(String username);
}
