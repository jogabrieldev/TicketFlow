package com.ticketApi.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTest {

    private static final String SENHA_HASH = "$2a$10$hashDeSenhaApenasParaTeste";

    @Test
    void deveCriarUsuarioComDadosValidos() {
        User usuario = new User(
                " Maria Silva ",
                " Maria.Silva@Exemplo.com ",
                SENHA_HASH,
                UserRole.CLIENTE
        );

        assertThat(usuario.obterId()).isNotNull();
        assertThat(usuario.obterNome()).isEqualTo("Maria Silva");
        assertThat(usuario.obterEmail()).isEqualTo("maria.silva@exemplo.com");
        assertThat(usuario.obterSenhaHash()).isEqualTo(SENHA_HASH);
        assertThat(usuario.obterPapel()).isEqualTo(UserRole.CLIENTE);
    }

    @Test
    void deveRejeitarNomeVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(" ", "maria@exemplo.com", SENHA_HASH, UserRole.CLIENTE))
                .withMessage("O nome do usuário é obrigatório");
    }

    @Test
    void deveRejeitarNomeMaiorQueOLimiteDoBanco() {
        String nomeLongo = "a".repeat(256);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User(nomeLongo, "maria@exemplo.com", SENHA_HASH, UserRole.CLIENTE))
                .withMessage("O nome do usuário deve ter no máximo 255 caracteres");
    }

    @Test
    void deveRejeitarEmailVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User("Maria Silva", " ", SENHA_HASH, UserRole.CLIENTE))
                .withMessage("O e-mail do usuário é obrigatório");
    }

    @Test
    void deveRejeitarEmailComFormatoInvalido() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User("Maria Silva", "email-invalido", SENHA_HASH, UserRole.CLIENTE))
                .withMessage("O e-mail do usuário possui formato inválido");
    }

    @Test
    void deveRejeitarHashDeSenhaVazio() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User("Maria Silva", "maria@exemplo.com", " ", UserRole.CLIENTE))
                .withMessage("O hash da senha é obrigatório");
    }

    @Test
    void deveRejeitarPapelNulo() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new User("Maria Silva", "maria@exemplo.com", SENHA_HASH, null))
                .withMessage("O papel do usuário é obrigatório");
    }
}
