package com.example.healthcare.repository;

import com.example.healthcare.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link User} entity persistence and lookup.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    java.util.List<User> findAllByHospitalId(Long hospitalId);

    Optional<User> findByIdAndHospitalId(Long id, Long hospitalId);
}
