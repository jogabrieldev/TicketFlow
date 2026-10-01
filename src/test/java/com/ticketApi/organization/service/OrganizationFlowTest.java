package com.ticketApi.organization.service;

import com.ticketApi.event.dto.CreateEventRequest;
import com.ticketApi.event.dto.EventResponse;
import com.ticketApi.event.service.EventService;
import com.ticketApi.organization.dto.CreateOrganizationRequest;
import com.ticketApi.organization.dto.OrganizationResponse;
import com.ticketApi.organization.entity.OrganizationMemberRole;
import com.ticketApi.organization.repository.OrganizationMemberRepository;
import com.ticketApi.reservation.dto.CreateReservationItemRequest;
import com.ticketApi.reservation.dto.CreateReservationRequest;
import com.ticketApi.reservation.dto.ReservationResponse;
import com.ticketApi.reservation.service.ReservationService;
import com.ticketApi.ticket.dto.CreateTicketBatchRequest;
import com.ticketApi.ticket.dto.TicketBatchResponse;
import com.ticketApi.ticket.service.TicketBatchService;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "ticketflow.reservation.expiration.enabled=false")
@Transactional
class OrganizationFlowTest {

    @Autowired
    private OrganizationService servicoDeOrganizacoes;

    @Autowired
    private EventService servicoDeEventos;

    @Autowired
    private TicketBatchService servicoDeLotes;

    @Autowired
    private ReservationService servicoDeReservas;

    @Autowired
    private OrganizationMemberRepository repositorioDeMembros;

    @Autowired
    private UserRepository repositorioDeUsuarios;

    @Test
    void deveExecutarFluxoAdministrativoEPermitirCompraDoClienteSemVinculoOrganizacional() {
        User proprietario = salvarUsuario("proprietario", UserRole.ADMINISTRADOR);
        User administradorGlobal = salvarUsuario("administrador-global", UserRole.ADMINISTRADOR);
        User cliente = salvarUsuario("cliente", UserRole.CLIENTE);

        OrganizationResponse organizacao = servicoDeOrganizacoes.criar(
                proprietario.obterEmail(),
                criarRequisicaoDeOrganizacao()
        );
        EventResponse evento = servicoDeEventos.criar(
                administradorGlobal.obterEmail(),
                organizacao.id(),
                new CreateEventRequest(
                        "Java Conference",
                        "Evento de tecnologia",
                        "Centro de Convenções",
                        OffsetDateTime.parse("2027-10-10T09:00:00-03:00"),
                        OffsetDateTime.parse("2027-10-10T18:00:00-03:00")
                )
        );
        TicketBatchResponse lote = servicoDeLotes.criar(
                administradorGlobal.obterEmail(),
                evento.id(),
                new CreateTicketBatchRequest("Primeiro lote", new BigDecimal("100.00"), 10)
        );
        ReservationResponse reserva = servicoDeReservas.criar(
                cliente.obterEmail(),
                new CreateReservationRequest(List.of(new CreateReservationItemRequest(lote.id(), 2)))
        );

        assertThat(evento.organizacaoId()).isEqualTo(organizacao.id());
        assertThat(lote.eventoId()).isEqualTo(evento.id());
        assertThat(reserva.id()).isNotNull();
        assertThat(reserva.eventoId()).isEqualTo(evento.id());
        assertThat(repositorioDeMembros.existePorOrganizacaoIdEUsuarioIdEPapel(
                organizacao.id(),
                proprietario.obterId(),
                OrganizationMemberRole.PROPRIETARIO
        )).isTrue();
        assertThat(repositorioDeMembros.existePorUsuarioIdEPapel(
                cliente.obterId(),
                OrganizationMemberRole.PROPRIETARIO
        )).isFalse();
    }

    private User salvarUsuario(String prefixo, UserRole papel) {
        return repositorioDeUsuarios.saveAndFlush(new User(
                "Usuário " + prefixo,
                prefixo + "-" + UUID.randomUUID() + "@exemplo.com",
                "hash-seguro",
                papel
        ));
    }

    private CreateOrganizationRequest criarRequisicaoDeOrganizacao() {
        return new CreateOrganizationRequest(
                "Ticket Flow",
                "Ticket Flow Tecnologia Ltda",
                "11.222.333/0001-81",
                "contato@ticketflow.com.br",
                "(11) 98765-4321",
                "Avenida Paulista",
                "1000",
                null,
                "Bela Vista",
                "São Paulo",
                "SP",
                "01310-100"
        );
    }
}
