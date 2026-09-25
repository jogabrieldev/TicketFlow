package com.ticketApi.auth.service;

import com.ticketApi.user.entity.User;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository repositorioDeUsuarios;

    public DatabaseUserDetailsService(UserRepository repositorioDeUsuarios) {
        this.repositorioDeUsuarios = repositorioDeUsuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User usuario = repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.obterEmail())
                .password(usuario.obterSenhaHash())
                .authorities("ROLE_" + usuario.obterPapel().name())
                .build();
    }
}
