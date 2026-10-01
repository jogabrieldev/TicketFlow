package com.ticketApi.event.service;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventPageResponse;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.entity.Event;
import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.event.repository.EventRepository;
import com.ticketApi.organization.entity.Organization;
import com.ticketApi.organization.exception.OrganizationAccessDeniedException;
import com.ticketApi.organization.exception.OrganizationNotFoundException;
import com.ticketApi.organization.repository.OrganizationRepository;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EventService {

    private final EventRepository repositorioDeEventos;
    private final OrganizationRepository repositorioDeOrganizacoes;
    private final UserRepository repositorioDeUsuarios;

    public EventService(
            EventRepository repositorioDeEventos,
            OrganizationRepository repositorioDeOrganizacoes,
            UserRepository repositorioDeUsuarios
    ) {
        this.repositorioDeEventos = repositorioDeEventos;
        this.repositorioDeOrganizacoes = repositorioDeOrganizacoes;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
    }

    @Transactional
    public EventResponse criar(String emailDoUsuario, UUID organizacaoId, CreateEventRequest requisicao) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        Organization organizacao = repositorioDeOrganizacoes.findById(organizacaoId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizacaoId));
        validarPermissaoDeCriacao(usuario, organizacaoId);

        Event evento = new Event(
                organizacao,
                requisicao.nome(),
                requisicao.descricao(),
                requisicao.local(),
                requisicao.inicioEm(),
                requisicao.terminoEm()
        );

        return EventResponse.de(repositorioDeEventos.save(evento));
    }

    @Transactional(readOnly = true)
    public EventResponse buscarPorId(UUID eventoId) {
        return repositorioDeEventos.findById(eventoId)
                .map(EventResponse::de)
                .orElseThrow(() -> new EventNotFoundException(eventoId));
    }

    @Transactional(readOnly = true)
    public EventPageResponse listar(int pagina, int tamanho) {
        PageRequest requisicaoDePagina = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.ASC, "inicioEm")
        );
        Page<EventResponse> eventos = repositorioDeEventos.findAll(requisicaoDePagina).map(EventResponse::de);

        return EventPageResponse.de(eventos);
    }

    private User buscarUsuarioAutenticado(String email) {
        return repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(email));
    }

    private void validarPermissaoDeCriacao(User usuario, UUID organizacaoId) {
        if (usuario.obterPapel() != UserRole.ADMINISTRADOR) {
            throw new OrganizationAccessDeniedException(organizacaoId);
        }
    }
}
