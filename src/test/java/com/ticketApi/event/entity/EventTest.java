package com.ticketApi.event.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static com.ticketApi.organization.OrganizationTestFactory.criarOrganizacao;

class EventTest {

    private static final OffsetDateTime INICIO_EM =
            OffsetDateTime.parse("2026-10-10T09:00:00-03:00");
    private static final OffsetDateTime TERMINO_EM =
            OffsetDateTime.parse("2026-10-10T18:00:00-03:00");

    @Test
    void deveRejeitarEventoSemOrganizacao() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        null, "Java Conference", null, "Convention Center", INICIO_EM, TERMINO_EM))
                .withMessage("A organização do evento é obrigatória");
    }

    @Test
    void deveCriarEventoComDadosValidos() {
        Event evento = new Event(
                criarOrganizacao(),
                " Java Conference ",
                " Conference about the Java ecosystem ",
                " Convention Center ",
                INICIO_EM,
                TERMINO_EM
        );

        assertThat(evento.obterId()).isNotNull();
        assertThat(evento.obterNome()).isEqualTo("Java Conference");
        assertThat(evento.obterDescricao()).isEqualTo("Conference about the Java ecosystem");
        assertThat(evento.obterLocal()).isEqualTo("Convention Center");
        assertThat(evento.obterInicioEm()).isEqualTo(INICIO_EM);
        assertThat(evento.obterTerminoEm()).isEqualTo(TERMINO_EM);
    }

    @Test
    void deveNormalizarDescricaoVaziaParaNulo() {
        Event evento = new Event(
                criarOrganizacao(), "Java Conference", "  ", "Convention Center", INICIO_EM, TERMINO_EM);

        assertThat(evento.obterDescricao()).isNull();
    }

    @Test
    void deveRejeitarNomeVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), " ", null, "Convention Center", INICIO_EM, TERMINO_EM))
                .withMessage("O nome do evento é obrigatório");
    }

    @Test
    void deveRejeitarNomeMaiorQueOLimiteDoBanco() {
        String nomeLongo = "a".repeat(256);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), nomeLongo, null, "Convention Center", INICIO_EM, TERMINO_EM))
                .withMessage("O nome do evento deve ter no máximo 255 caracteres");
    }

    @Test
    void deveRejeitarLocalVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), "Java Conference", null, " ", INICIO_EM, TERMINO_EM))
                .withMessage("O local do evento é obrigatório");
    }

    @Test
    void deveRejeitarDataDeInicioNula() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), "Java Conference", null, "Convention Center", null, TERMINO_EM))
                .withMessage("A data de início do evento é obrigatória");
    }

    @Test
    void deveRejeitarDataDeTerminoNula() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), "Java Conference", null, "Convention Center", INICIO_EM, null))
                .withMessage("A data de término do evento é obrigatória");
    }

    @Test
    void deveRejeitarTerminoIgualAoInicio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(), "Java Conference", null, "Convention Center", INICIO_EM, INICIO_EM))
                .withMessage("A data de término do evento deve ser posterior à data de início");
    }

    @Test
    void deveRejeitarTerminoAnteriorAoInicio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Event(
                        criarOrganizacao(),
                        "Java Conference",
                        null,
                        "Convention Center",
                        INICIO_EM,
                        INICIO_EM.minusMinutes(1)
                ))
                .withMessage("A data de término do evento deve ser posterior à data de início");
    }
}
