package com.ticketApi.shared.idempotency;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException() {
        super("A chave de idempotência já foi utilizada com uma requisição diferente");
    }
}
