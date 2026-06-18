package com.project.klare_server.auth.repository;

import com.project.klare_server.auth.domain.BusinessUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessUserRepository extends JpaRepository<BusinessUser, UUID> {

    Optional<BusinessUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
