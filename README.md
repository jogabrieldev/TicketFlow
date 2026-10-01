# TicketFlow

TicketFlow é uma API REST para gerenciamento e venda de ingressos. A aplicação cobre o fluxo principal desde a criação de organizações, eventos e lotes até a reserva de ingressos, geração do pedido e processamento simulado do pagamento.

O projeto foi desenvolvido como um monólito modular em Java e Spring Boot, com foco em regras de negócio, consistência dos dados e proteção contra operações concorrentes.

## Funcionalidades

- Cadastro e autenticação de usuários.
- Gerenciamento global de organizações pelo perfil MASTER.
- Cadastro de administradores vinculados a uma única organização.
- Criação e consulta de eventos e lotes de ingressos.
- Reserva temporária de ingressos com controle de estoque.
- Expiração automática de reservas e devolução dos ingressos ao lote.
- Criação de pedidos a partir de reservas ativas.
- Processamento simulado de pagamentos aprovados, recusados ou em processamento.
- Idempotência nas operações de reserva, pedido e pagamento.
- Paginação e respostas de erro padronizadas.
- Documentação interativa com OpenAPI e Swagger UI.

## Fluxo principal

O MASTER cadastra uma organização e cria sua conta administrativa. O administrador cria eventos e lotes exclusivamente para a organização à qual está vinculado. Os eventos e lotes podem ser consultados publicamente.

O cliente cria uma reserva, que reduz o estoque disponível de forma atômica. A reserva possui um prazo de expiração; caso não seja paga, uma tarefa agendada libera os ingressos novamente. A partir de uma reserva ativa, o cliente cria um pedido e realiza o pagamento. Quando aprovado, o pedido e a reserva são confirmados.

As operações críticas exigem uma `Idempotency-Key`, gerada pelo consumidor da API e reutilizada em tentativas da mesma operação. Isso evita reservas, pedidos ou pagamentos duplicados em casos de timeout e repetição da requisição.

## Estrutura do projeto

O código está organizado por domínio:

```text
src/main/java/com/ticketApi
├── auth            # autenticação e configuração de segurança
├── user            # usuários e cadastro
├── organization    # organizações e seus responsáveis
├── event           # eventos
├── ticket          # lotes, preços e disponibilidade
├── reservation     # reservas, concorrência e expiração
├── order           # pedidos e itens
├── payment         # pagamentos e gateway simulado
└── shared          # configurações, erros e idempotência
```

Cada domínio utiliza, conforme necessário, controllers, DTOs, services, entidades, repositories e exceções próprias. As alterações do banco são versionadas com Flyway em `src/main/resources/db/migration`.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA e Hibernate
- Spring Security com HTTP Basic
- PostgreSQL
- Flyway
- Bean Validation
- SpringDoc OpenAPI
- JUnit, Mockito e Testcontainers

## Requisitos

- Java 21
- PostgreSQL 16 ou versão compatível
- Docker para executar os testes de integração baseados em Testcontainers

## Configuração

Por padrão, a aplicação utiliza o banco local abaixo:

```properties
spring.datasource.url="{Url-de-conexão}"
spring.datasource.username="${Nome-banco}"
spring.datasource.password="${senha}"
```

Crie o banco `db_ticket_flow` antes de iniciar a aplicação. O Flyway executará automaticamente as migrations e o Hibernate validará o esquema.

Para outros ambientes, sobrescreva essas propriedades por variáveis de ambiente ou por um perfil externo do Spring. Credenciais de produção não devem ser mantidas no repositório.

O cadastro público cria usuários com o papel de cliente. O MASTER é provisionado administrativamente e pode criar organizações por `POST /api/organizations` e seus administradores por `POST /api/organizations/{organizacaoId}/administrators`.

As permissões administrativas seguem esta separação:

- `MASTER`: gerencia organizações e cria seus administradores; não cria eventos ou lotes.
- `ADMINISTRADOR`: pertence a uma organização e só pode criar eventos e lotes dentro dela.
- `CLIENTE`: realiza reservas, pedidos e pagamentos.

## Execução

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Em Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Após a inicialização, a documentação interativa estará disponível em:

```text
http://localhost:8080/swagger-ui/index.html
```

As operações protegidas utilizam autenticação HTTP Basic. Em qualquer ambiente publicado, a API deve ser acessada exclusivamente por HTTPS.

## Testes

Para executar a suíte:

```powershell
.\mvnw.cmd test
```

Parte dos testes utiliza PostgreSQL por meio de Testcontainers, portanto o Docker deve estar em execução. O projeto possui testes de unidade, controllers, persistência, fluxo completo, concorrência de estoque, expiração de reservas e idempotência.

## Decisões de negócio importantes

- O estoque é reduzido no momento da reserva, e não apenas no pagamento.
- Reservas vencidas devolvem automaticamente os ingressos ao lote.
- Uma reserva só pode originar um pedido.
- Pagamentos recusados permitem nova tentativa; pagamentos aprovados ou em processamento bloqueiam outra tentativa ativa.
- A mesma chave de idempotência com o mesmo conteúdo retorna o recurso já criado; com conteúdo diferente, gera conflito.
- Datas são persistidas em UTC e apresentadas conforme a configuração de fuso horário da aplicação.
