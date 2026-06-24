package com.project.klare_server.personal.repository;

import com.project.klare_server.personal.domain.PersonalUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalUserRepository extends JpaRepository<PersonalUser, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<PersonalUser> findByEmailIgnoreCase(String email);
}
