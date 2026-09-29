package com.ticketApi.shared.idempotency;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyRepository repositorio;
    private final EntityManager gerenciadorDeEntidades;

    public IdempotencyService(IdempotencyRepository repositorio, EntityManager gerenciadorDeEntidades) {
        this.repositorio = repositorio;
        this.gerenciadorDeEntidades = gerenciadorDeEntidades;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T executar(
            UUID usuarioId,
            IdempotencyOperation operacao,
            String chave,
            String hashDaRequisicao,
            Function<UUID, T> recuperar,
            Supplier<CreatedResource<T>> criar
    ) {
        validarChave(chave);
        String escopo = usuarioId + "|" + operacao.name() + "|" + chave;
        gerenciadorDeEntidades.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(CAST(?1 AS text), 0))"
                )
                .setParameter(1, escopo)
                .getSingleResult();

        var existente = repositorio.findByUsuarioIdAndOperacaoAndChave(usuarioId, operacao, chave);
        if (existente.isPresent()) {
            IdempotencyRecord registro = existente.get();
            if (!registro.obterHashDaRequisicao().equals(hashDaRequisicao)) {
                throw new IdempotencyConflictException();
            }
            return recuperar.apply(registro.obterRecursoId());
        }

        CreatedResource<T> criado = criar.get();
        repositorio.saveAndFlush(new IdempotencyRecord(
                usuarioId,
                operacao,
                chave,
                hashDaRequisicao,
                criado.recursoId()
        ));
        return criado.resposta();
    }

    private static void validarChave(String chave) {
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A chave de idempotência é obrigatória");
        }
        if (chave.length() > 255) {
            throw new IllegalArgumentException("A chave de idempotência deve ter no máximo 255 caracteres");
        }
    }

    public record CreatedResource<T>(UUID recursoId, T resposta) {
    }
}
