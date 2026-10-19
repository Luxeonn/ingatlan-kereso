package ingatlanok.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ingatlanok.persistence.entity.ConfigEntity;

@Repository
public interface ConfigRepository extends JpaRepository<ConfigEntity, String> {}
