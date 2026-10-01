package com.ticketApi.ticket.entity;

import com.ticketApi.event.entity.Event;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TicketBatchTest {

    @Test
    void deveCriarLoteComDadosValidos() {
        Event evento = criarEvento();

        TicketBatch lote = new TicketBatch(evento, " Primeiro lote ", new BigDecimal("100.00"), 500);

        assertThat(lote.obterId()).isNotNull();
        assertThat(lote.obterEvento()).isSameAs(evento);
        assertThat(lote.obterNome()).isEqualTo("Primeiro lote");
        assertThat(lote.obterPreco()).isEqualByComparingTo("100.00");
        assertThat(lote.obterQuantidadeTotal()).isEqualTo(500);
        assertThat(lote.obterQuantidadeDisponivel()).isEqualTo(500);
    }

    @Test
    void deveRejeitarEventoNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(null, "Primeiro lote", new BigDecimal("100.00"), 500))
                .withMessage("O evento do lote é obrigatório");
    }

    @Test
    void deveRejeitarNomeVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(criarEvento(), " ", new BigDecimal("100.00"), 500))
                .withMessage("O nome do lote é obrigatório");
    }

    @Test
    void deveRejeitarNomeMaiorQueOLimiteDoBanco() {
        String nomeLongo = "a".repeat(256);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(criarEvento(), nomeLongo, new BigDecimal("100.00"), 500))
                .withMessage("O nome do lote deve ter no máximo 255 caracteres");
    }

    @Test
    void deveRejeitarPrecoNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(criarEvento(), "Primeiro lote", null, 500))
                .withMessage("O preço do lote é obrigatório");
    }

    @Test
    void deveRejeitarPrecoIgualAZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(criarEvento(), "Primeiro lote", BigDecimal.ZERO, 500))
                .withMessage("O preço do lote deve ser maior que zero");
    }

    @Test
    void deveRejeitarPrecoComMaisDeDuasCasasDecimais() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(
                        criarEvento(),
                        "Primeiro lote",
                        new BigDecimal("100.001"),
                        500
                ))
                .withMessage("O preço do lote deve ter no máximo duas casas decimais");
    }

    @Test
    void deveRejeitarPrecoAcimaDaPrecisaoDoBanco() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(
                        criarEvento(),
                        "Primeiro lote",
                        new BigDecimal("10000000000.00"),
                        500
                ))
                .withMessage("O preço do lote excede o valor máximo permitido");
    }

    @Test
    void deveRejeitarQuantidadeIgualAZero() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TicketBatch(
                        criarEvento(),
                        "Primeiro lote",
                        new BigDecimal("100.00"),
                        0
                ))
                .withMessage("A quantidade total do lote deve ser maior que zero");
    }

    private Event criarEvento() {
        OffsetDateTime inicioEm = OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
        OffsetDateTime terminoEm = OffsetDateTime.parse("2026-10-10T18:00:00-03:00");

        return new Event(
                com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao(),
                "Java Conference", null, "Centro de Convenções", inicioEm, terminoEm);
    }
}
