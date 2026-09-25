package com.ticketApi.reservation.entity;

import com.ticketApi.event.entity.Event;
import com.ticketApi.ticket.entity.TicketBatch;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-09-25T11:00:00-03:00");
    private static final OffsetDateTime EXPIRA_EM = AGORA.plusMinutes(15);

    @Test
    void deveCriarReservaPendente() {
        User usuario = criarUsuario();

        Reservation reserva = new Reservation(usuario, EXPIRA_EM, AGORA);

        assertThat(reserva.obterId()).isNotNull();
        assertThat(reserva.obterUsuario()).isSameAs(usuario);
        assertThat(reserva.obterStatus()).isEqualTo(ReservationStatus.PENDENTE);
        assertThat(reserva.obterExpiraEm()).isEqualTo(EXPIRA_EM);
        assertThat(reserva.obterItens()).isEmpty();
    }

    @Test
    void deveRejeitarUsuarioNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(null, EXPIRA_EM, AGORA))
                .withMessage("O usuário da reserva é obrigatório");
    }

    @Test
    void deveRejeitarExpiracaoNula() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(criarUsuario(), null, AGORA))
                .withMessage("A data de expiração da reserva é obrigatória");
    }

    @Test
    void deveRejeitarExpiracaoIgualAoMomentoDeCriacao() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Reservation(criarUsuario(), AGORA, AGORA))
                .withMessage("A data de expiração deve ser posterior à criação da reserva");
    }

    @Test
    void deveAdicionarItemECapturarPrecoDoLote() {
        Reservation reserva = criarReserva();
        TicketBatch lote = criarLote();

        reserva.adicionarItem(lote, 2);

        ReservationItem item = reserva.obterItens().getFirst();
        assertThat(item.obterId()).isNotNull();
        assertThat(item.obterReserva()).isSameAs(reserva);
        assertThat(item.obterLote()).isSameAs(lote);
        assertThat(item.obterQuantidade()).isEqualTo(2);
        assertThat(item.obterPrecoUnitario()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveRejeitarLoteNulo() {
        Reservation reserva = criarReserva();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> reserva.adicionarItem(null, 1))
                .withMessage("O lote do item é obrigatório");
    }

    @Test
    void deveRejeitarQuantidadeIgualAZero() {
        Reservation reserva = criarReserva();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> reserva.adicionarItem(criarLote(), 0))
                .withMessage("A quantidade do item deve ser maior que zero");
    }

    @Test
    void deveRejeitarMesmoLoteDuasVezes() {
        Reservation reserva = criarReserva();
        TicketBatch lote = criarLote();
        reserva.adicionarItem(lote, 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> reserva.adicionarItem(lote, 2))
                .withMessage("O lote já foi adicionado à reserva");
    }

    @Test
    void deveExporListaDeItensSomenteParaLeitura() {
        Reservation reserva = criarReserva();
        reserva.adicionarItem(criarLote(), 1);

        assertThatThrownBy(() -> reserva.obterItens().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private Reservation criarReserva() {
        return new Reservation(criarUsuario(), EXPIRA_EM, AGORA);
    }

    private User criarUsuario() {
        return new User(
                "Maria Silva",
                "maria@exemplo.com",
                "hash-da-senha",
                UserRole.CLIENTE
        );
    }

    private TicketBatch criarLote() {
        OffsetDateTime inicioEm = OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
        OffsetDateTime terminoEm = OffsetDateTime.parse("2026-10-10T18:00:00-03:00");
        Event evento = new Event("Java Conference", null, "Centro de Convenções", inicioEm, terminoEm);
        return new TicketBatch(evento, "Primeiro lote", new BigDecimal("100.00"), 500);
    }
}
