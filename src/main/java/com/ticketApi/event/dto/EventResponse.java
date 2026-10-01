package com.ticketApi.event.dto;

import com.ticketApi.event.entity.Event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID organizacaoId,
        String organizacaoNomeFantasia,
        String nome,
        String descricao,
        String local,
        OffsetDateTime inicioEm,
        OffsetDateTime terminoEm,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static EventResponse de(Event evento) {
        return new EventResponse(
                evento.obterId(),
                evento.obterOrganizacao().obterId(),
                evento.obterOrganizacao().obterNomeFantasia(),
                evento.obterNome(),
                evento.obterDescricao(),
                evento.obterLocal(),
                evento.obterInicioEm(),
                evento.obterTerminoEm(),
                evento.obterCriadoEm(),
                evento.obterAtualizadoEm()
        );
    }
}
