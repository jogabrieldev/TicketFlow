package com.ticketApi.user.controller;

import com.ticketApi.user.dto.CreateUserRequest;
import com.ticketApi.user.dto.UserResponse;
import com.ticketApi.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService servicoDeUsuarios;

    public UserController(UserService servicoDeUsuarios) {
        this.servicoDeUsuarios = servicoDeUsuarios;
    }

    @PostMapping
    public ResponseEntity<UserResponse> cadastrar(@Valid @RequestBody CreateUserRequest requisicao) {
        UserResponse usuario = servicoDeUsuarios.cadastrar(requisicao);

        return ResponseEntity
                .created(URI.create("/api/users/" + usuario.id()))
                .body(usuario);
    }
}
