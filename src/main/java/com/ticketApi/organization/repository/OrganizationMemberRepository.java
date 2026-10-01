package com.ticketApi.organization.repository;

import com.ticketApi.organization.entity.OrganizationMember;
import com.ticketApi.organization.entity.OrganizationMemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(membro) > 0 THEN true ELSE false END
              FROM OrganizationMember membro
             WHERE membro.usuario.id = :usuarioId
               AND membro.papel = :papel
            """)
    boolean existePorUsuarioIdEPapel(
            @Param("usuarioId") UUID usuarioId,
            @Param("papel") OrganizationMemberRole papel
    );

    @Query("""
            SELECT CASE WHEN COUNT(membro) > 0 THEN true ELSE false END
              FROM OrganizationMember membro
             WHERE membro.organizacao.id = :organizacaoId
               AND membro.usuario.id = :usuarioId
               AND membro.papel = :papel
            """)
    boolean existePorOrganizacaoIdEUsuarioIdEPapel(
            @Param("organizacaoId") UUID organizacaoId,
            @Param("usuarioId") UUID usuarioId,
            @Param("papel") OrganizationMemberRole papel
    );

    @Query("""
            SELECT membro
              FROM OrganizationMember membro
             WHERE membro.usuario.id = :usuarioId
               AND membro.papel = :papel
            """)
    Page<OrganizationMember> buscarPorUsuarioIdEPapel(
            @Param("usuarioId") UUID usuarioId,
            @Param("papel") OrganizationMemberRole papel,
            Pageable paginacao
    );
}
