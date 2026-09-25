package com.ticketApi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationTests {

	@Autowired
	private MockMvc simuladorMvc;

	@Test
	void contextoCarrega() {
	}

	@Test
	void deveDisponibilizarDocumentacaoOpenApiSemAutenticacao() throws Exception {
		simuladorMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.info.title").value("TicketFlow API"))
				.andExpect(jsonPath("$.components.securitySchemes.autenticacaoBasica.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.autenticacaoBasica.scheme").value("basic"))
				.andExpect(jsonPath("$['paths']['/api/events']['post']['security'][0]['autenticacaoBasica']").isArray())
				.andExpect(jsonPath(
						"$['paths']['/api/events/{eventoId}/ticket-batches']['post']['security'][0]['autenticacaoBasica']"
				).isArray())
				.andExpect(jsonPath("$['paths']['/api/events']['get']['security']").doesNotExist());
	}

	@Test
	void deveDisponibilizarInterfaceDoSwaggerSemAutenticacao() throws Exception {
		simuladorMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk());
	}

}
