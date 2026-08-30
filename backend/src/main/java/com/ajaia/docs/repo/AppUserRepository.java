package com.ajaia.docs.repo;

import com.ajaia.docs.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findAllByOrderByDisplayNameAsc();
}
