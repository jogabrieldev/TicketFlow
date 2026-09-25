package com.ticketApi.user.repository;

import com.ticketApi.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT usuario FROM User usuario WHERE usuario.email = LOWER(:email)")
    Optional<User> buscarPorEmail(@Param("email") String email);
}
