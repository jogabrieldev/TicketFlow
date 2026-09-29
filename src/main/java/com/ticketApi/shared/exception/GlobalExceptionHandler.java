package com.ticketApi.shared.exception;

import com.ticketApi.event.exception.EventNotFoundException;
import com.ticketApi.order.exception.OrderAlreadyExistsException;
import com.ticketApi.order.exception.ReservationForOrderNotFoundException;
import com.ticketApi.order.exception.ReservationUnavailableForOrderException;
import com.ticketApi.payment.exception.PaymentNotFoundException;
import com.ticketApi.payment.exception.PaymentOrderNotFoundException;
import com.ticketApi.payment.exception.PaymentUnavailableException;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.reservation.exception.DuplicateTicketBatchException;
import com.ticketApi.reservation.exception.EmptyReservationException;
import com.ticketApi.reservation.exception.MixedEventReservationException;
import com.ticketApi.shared.idempotency.IdempotencyConflictException;
import com.ticketApi.ticket.exception.InsufficientTicketAvailabilityException;
import com.ticketApi.ticket.exception.TicketBatchNotFoundException;
import com.ticketApi.user.exception.EmailAlreadyRegisteredException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    ProblemDetail tratarEventoNaoEncontrado(EventNotFoundException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(TicketBatchNotFoundException.class)
    ProblemDetail tratarLoteNaoEncontrado(TicketBatchNotFoundException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(ReservationForOrderNotFoundException.class)
    ProblemDetail tratarReservaDoPedidoNaoEncontrada(ReservationForOrderNotFoundException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler({OrderAlreadyExistsException.class, ReservationUnavailableForOrderException.class})
    ProblemDetail tratarConflitoDePedido(RuntimeException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Pedido não pode ser criado");
        return problema;
    }

    @ExceptionHandler({PaymentNotFoundException.class, PaymentOrderNotFoundException.class})
    ProblemDetail tratarPagamentoNaoEncontrado(RuntimeException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(PaymentUnavailableException.class)
    ProblemDetail tratarPagamentoIndisponivel(PaymentUnavailableException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Pagamento não pode ser processado");
        return problema;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    ProblemDetail tratarConflitoDeIdempotencia(IdempotencyConflictException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Conflito de idempotência");
        return problema;
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    ProblemDetail tratarUsuarioAutenticadoNaoEncontrado(AuthenticatedUserNotFoundException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, excecao.getMessage());
        problema.setTitle("Recurso não encontrado");
        return problema;
    }

    @ExceptionHandler(InsufficientTicketAvailabilityException.class)
    ProblemDetail tratarQuantidadeIndisponivel(InsufficientTicketAvailabilityException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("Ingressos indisponíveis");
        return problema;
    }

    @ExceptionHandler({
            DuplicateTicketBatchException.class,
            EmptyReservationException.class,
            MixedEventReservationException.class
    })
    ProblemDetail tratarReservaInvalida(RuntimeException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        problema.setTitle("Reserva inválida");
        return problema;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail tratarEmailJaCadastrado(EmailAlreadyRegisteredException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, excecao.getMessage());
        problema.setTitle("E-mail já cadastrado");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail tratarArgumentoDeMetodoInvalido(MethodArgumentNotValidException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos da requisição são inválidos"
        );
        problema.setTitle("Requisição inválida");

        List<Map<String, String>> erros = excecao.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", erro.getDefaultMessage() == null ? "Valor inválido" : erro.getDefaultMessage()
                ))
                .toList();
        problema.setProperty("erros", erros);

        return problema;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail tratarViolacaoDeRestricao(ConstraintViolationException excecao) {
        String detalhes = excecao.getConstraintViolations().stream()
                .map(violacao -> violacao.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalhes);
        problema.setTitle("Parâmetro de requisição inválido");
        return problema;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail tratarValidacaoDeMetodo(HandlerMethodValidationException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais parâmetros da requisição são inválidos"
        );
        problema.setTitle("Parâmetro de requisição inválido");
        return problema;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tratarTipoDeArgumentoInvalido(MethodArgumentTypeMismatchException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O parâmetro '" + excecao.getName() + "' possui um valor inválido"
        );
        problema.setTitle("Parâmetro de requisição inválido");
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail tratarCorpoIlegivel(HttpMessageNotReadableException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "O corpo da requisição está ausente ou possui formato inválido"
        );
        problema.setTitle("Requisição inválida");
        return problema;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail tratarArgumentoIlegal(IllegalArgumentException excecao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, excecao.getMessage());
        problema.setTitle("Violação de regra de negócio");
        return problema;
    }
}
