package com.cotacao.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.model.RecordType;

@Service
public class ComprasGovIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(ComprasGovIntegrationService.class);
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ComprasGovIntegrationService(WebClient comprasGovWebClient) {
        this.webClient = comprasGovWebClient;
    }

    public List<PriceRecord> fetchPricesFromGov(String catmatCode) {
        List<PriceRecord> allRecords = new ArrayList<>();
        String codigoLimpo = catmatCode.trim();

        // 📅 CÁLCULO DINÂMICO DOS PARÂMETROS OBRIGATÓRIOS (Últimos 12 meses)
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate umAnoAtras = hoje.minusYears(1);
        
        java.time.format.DateTimeFormatter formatador = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dataMin = umAnoAtras.format(formatador);
        String dataMax = hoje.format(formatador);

        // 1️⃣ ENDPOINT HISTÓRICO: Pesquisa de Preços Praticados (Compras.gov)
        String urlHistorico = "https://dadosabertos.compras.gov.br/modulo-pesquisa-preco/1_consultarMaterial"
                            + "?pagina=1&tamanhoPagina=10&codigoItemCatalogo=" + codigoLimpo;

        // 2️⃣ ENDPOINT ATAS (ARP): Injetando os campos obrigatórios descobertos no Swagger!
        String urlAtas = "https://dadosabertos.compras.gov.br/modulo-arp/1_consultarARP"
                       + "?pagina=1&tamanhoPagina=10&codigoItemCatalogo=" + codigoLimpo
                       + "&dataVigenciaInicialMin=" + dataMin
                       + "&dataVigenciaInicialMax=" + dataMax;

        // Chamada 1: Compras Históricas
        try {
            logger.info(">>>>>>>> [MÁRCIO] COLETANDO COMPRAS HISTÓRICAS PARA O CATMAT: {}", codigoLimpo);
            String resHistorico = executeGetRequest(urlHistorico);
            JsonNode rootHistorico = mapper.readTree(resHistorico);
            allRecords.addAll(extractRecordsReal(rootHistorico, codigoLimpo, RecordType.HISTORICAL_PURCHASE));
        } catch (Exception e) {
            logger.warn(">>>> [MÁRCIO] Alerta na rota de histórico: {}", e.getMessage());
        }

        // Chamada 2: Atas de Registro de Preços Ativas (Módulo ARP do Compras.gov)
        try {
            logger.info(">>>>>>>> [MÁRCIO] COLETANDO ARPs VIGENTES NO COMPRAS.GOV (Período: {} até {}): {}", dataMin, dataMax, codigoLimpo);
            String resAtas = executeGetRequest(urlAtas);
            JsonNode rootAtas = mapper.readTree(resAtas);
            allRecords.addAll(extractRecordsReal(rootAtas, codigoLimpo, RecordType.ACTIVE_ARP));
        } catch (Exception e) {
            logger.warn(">>>> [MÁRCIO] Alerta na rota de ARPs: {}", e.getMessage());
        }

        return allRecords;
    }

    /**
     * Executa a requisição GET injetando cabeçalhos de Media Type aceitos pelo Governo
     */
    private String executeGetRequest(String url) {
        return this.webClient.get()
                .uri(url)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    /**
     * Processador dinâmico preparado para normalizar as respostas das duas APIs
     */
    private List<PriceRecord> extractRecordsReal(JsonNode rootNode, String catmatCode, RecordType targetType) {
        List<PriceRecord> records = new ArrayList<>();
        
        JsonNode itens = rootNode.has("resultado") ? rootNode.get("resultado") : 
                         (rootNode.has("data") ? rootNode.get("data") : rootNode);

        if (itens != null && itens.isArray() && itens.size() > 0) {
            logger.info(">>>>>>>> [MÁRCIO] Processando {} itens para o tipo {}", itens.size(), targetType);
            
            for (JsonNode item : itens) {
                PriceRecord record = new PriceRecord();
                record.setCatmatCode(catmatCode);
                record.setRecordType(targetType);
                record.setResearchDate(LocalDateTime.now());
                record.setExcludedByCriticalAnalyses(false);

                // 🎯 CONDIÇÃO ESPECIAL PARA O MÓDULO DE ATAS (ARP) VISTO NO NAVEGADOR
                if (item.has("numeroAtaRegistroPreco")) {
                    
                    // Captura o valor global registrado na Ata
                    if (item.has("valorTotal") && !item.get("valorTotal").isNull()) {
                        record.setUnitPrice(new java.math.BigDecimal(item.get("valorTotal").asText()));
                    } else {
                        record.setUnitPrice(java.math.BigDecimal.ZERO);
                    }
                    
                    // Captura o objeto descritivo da licitação
                    if (item.has("objeto") && !item.get("objeto").isNull()) {
                        record.setItemDescription(item.get("objeto").asText());
                    } else {
                        record.setItemDescription("Ata de Registro de Preços do CATMAT " + catmatCode);
                    }
                    
                    // Captura o Órgão Gerenciador e o número da Ata
                    String orgao = item.has("nomeOrgao") ? item.get("nomeOrgao").asText() : "Órgão Federal";
                    String numeroAta = item.get("numeroAtaRegistroPreco").asText();
                    record.setSourceName(orgao + " - ARP nº " + numeroAta);
                    
                } else {
                    // 🎯 FLUXO NORMAL PARA PESQUISA DE PREÇOS HISTÓRICOS (COMPRAS.GOV)
                    if (item.has("precoUnitario") && !item.get("precoUnitario").isNull()) {
                        record.setUnitPrice(new java.math.BigDecimal(item.get("precoUnitario").asText()));
                    } else if (item.has("valorUnitario") && !item.get("valorUnitario").isNull()) {
                        record.setUnitPrice(new java.math.BigDecimal(item.get("valorUnitario").asText()));
                    } else if (item.has("valorUnitarioEstimado") && !item.get("valorUnitarioEstimado").isNull()) {
                        record.setUnitPrice(new java.math.BigDecimal(item.get("valorUnitarioEstimado").asText()));
                    } else { 
                        continue; 
                    }

                    if (item.has("descricaoItem") && !item.get("descricaoItem").isNull()) {
                        record.setItemDescription(item.get("descricaoItem").asText());
                    } else {
                        record.setItemDescription("Material Geral (CATMAT " + catmatCode + ")");
                    }

                    String orgao = item.has("nomeUasg") ? item.get("nomeUasg").asText() : "Órgão Público";
                    String forma = item.has("forma") ? item.get("forma").asText() : "Licitação";
                    record.setSourceName(orgao + " (" + forma + ")");
                }
                
                records.add(record);
            }
        }
        return records;
    }
}