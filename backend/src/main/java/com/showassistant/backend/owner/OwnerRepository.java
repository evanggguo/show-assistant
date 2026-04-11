package com.showassistant.backend.owner;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TDD 5.1 — Owner 数据访问层
 * 提供 Owner 实体的 CRUD 操作
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {
}
