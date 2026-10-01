package com.ticketApi.organization.service;

import com.ticketApi.organization.dto.CreateOrganizationRequest;
import com.ticketApi.organization.dto.OrganizationPageResponse;
import com.ticketApi.organization.dto.OrganizationResponse;
import com.ticketApi.organization.entity.Organization;
import com.ticketApi.organization.entity.OrganizationMember;
import com.ticketApi.organization.entity.OrganizationMemberRole;
import com.ticketApi.organization.exception.CnpjAlreadyRegisteredException;
import com.ticketApi.organization.exception.OrganizationManagementDeniedException;
import com.ticketApi.organization.exception.OrganizationNotFoundException;
import com.ticketApi.organization.exception.OrganizationOwnerAlreadyExistsException;
import com.ticketApi.organization.repository.OrganizationMemberRepository;
import com.ticketApi.organization.repository.OrganizationRepository;
import com.ticketApi.reservation.exception.AuthenticatedUserNotFoundException;
import com.ticketApi.user.entity.User;
import com.ticketApi.user.entity.UserRole;
import com.ticketApi.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository repositorioDeOrganizacoes;
    private final OrganizationMemberRepository repositorioDeMembros;
    private final UserRepository repositorioDeUsuarios;

    public OrganizationService(
            OrganizationRepository repositorioDeOrganizacoes,
            OrganizationMemberRepository repositorioDeMembros,
            UserRepository repositorioDeUsuarios
    ) {
        this.repositorioDeOrganizacoes = repositorioDeOrganizacoes;
        this.repositorioDeMembros = repositorioDeMembros;
        this.repositorioDeUsuarios = repositorioDeUsuarios;
    }

    @Transactional
    public OrganizationResponse criar(String emailDoUsuario, CreateOrganizationRequest requisicao) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        validarAdministrador(usuario);
        String cnpjNormalizado = normalizarDocumento(requisicao.cnpj());

        if (repositorioDeMembros.existePorUsuarioIdEPapel(usuario.obterId(), OrganizationMemberRole.PROPRIETARIO)) {
            throw new OrganizationOwnerAlreadyExistsException(usuario.obterEmail());
        }
        if (repositorioDeOrganizacoes.existsByCnpj(cnpjNormalizado)) {
            throw new CnpjAlreadyRegisteredException(cnpjNormalizado);
        }

        Organization organizacao = new Organization(
                requisicao.nomeFantasia(),
                requisicao.razaoSocial(),
                requisicao.cnpj(),
                requisicao.email(),
                requisicao.telefone(),
                requisicao.logradouro(),
                requisicao.numero(),
                requisicao.complemento(),
                requisicao.bairro(),
                requisicao.cidade(),
                requisicao.estado(),
                requisicao.cep()
        );

        try {
            Organization organizacaoSalva = repositorioDeOrganizacoes.saveAndFlush(organizacao);
            OrganizationMember proprietario = new OrganizationMember(
                    organizacaoSalva,
                    usuario,
                    OrganizationMemberRole.PROPRIETARIO
            );
            repositorioDeMembros.saveAndFlush(proprietario);
            return OrganizationResponse.de(organizacaoSalva);
        } catch (DataIntegrityViolationException excecao) {
            throw converterConflitoDeIntegridade(excecao, usuario.obterEmail(), cnpjNormalizado);
        }
    }

    @Transactional(readOnly = true)
    public OrganizationResponse buscarPorId(String emailDoUsuario, UUID organizacaoId) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        validarAdministrador(usuario);
        Organization organizacao = repositorioDeOrganizacoes.findById(organizacaoId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizacaoId));
        return OrganizationResponse.de(organizacao);
    }

    @Transactional(readOnly = true)
    public OrganizationPageResponse listar(String emailDoUsuario, int pagina, int tamanho) {
        User usuario = buscarUsuarioAutenticado(emailDoUsuario);
        validarAdministrador(usuario);
        PageRequest paginacao = PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.ASC, "nomeFantasia")
        );
        Page<OrganizationResponse> organizacoes = repositorioDeOrganizacoes
                .findAll(paginacao)
                .map(OrganizationResponse::de);

        return OrganizationPageResponse.de(organizacoes);
    }

    private User buscarUsuarioAutenticado(String email) {
        return repositorioDeUsuarios.buscarPorEmail(email)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(email));
    }

    private static void validarAdministrador(User usuario) {
        if (usuario.obterPapel() != UserRole.ADMINISTRADOR) {
            throw new OrganizationManagementDeniedException();
        }
    }

    private static RuntimeException converterConflitoDeIntegridade(
            DataIntegrityViolationException excecao,
            String email,
            String cnpj
    ) {
        String mensagem = obterMensagemDaCausaRaiz(excecao);
        if (mensagem.contains("uk_organizations_cnpj")) {
            return new CnpjAlreadyRegisteredException(cnpj);
        }
        if (mensagem.contains("uk_organization_members_owner_user")) {
            return new OrganizationOwnerAlreadyExistsException(email);
        }
        return excecao;
    }

    private static String obterMensagemDaCausaRaiz(Throwable excecao) {
        Throwable causa = excecao;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        return causa.getMessage() == null ? "" : causa.getMessage();
    }

    private static String normalizarDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }
}
