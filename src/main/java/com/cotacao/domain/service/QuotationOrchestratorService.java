package com.cotacao.domain.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotacao.domain.dto.QuotationSummaryDTO;
import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.repository.PriceRecordRepository;

@Service
public class QuotationOrchestratorService {

	private final PriceCalculationService calculationService;
	private final PriceRecordRepository priceRecordRepository;
	private final ComprasGovIntegrationService comprasGovService;

	// 🎯 ADICIONADO: Injeção correta via construtor gerenciado pelo Spring Boot
	public QuotationOrchestratorService(PriceRecordRepository repository, 
										ComprasGovIntegrationService comprasService,
										PriceCalculationService calculationService) {
		this.priceRecordRepository = repository;
		this.comprasGovService = comprasService;
		this.calculationService = calculationService;
	}

	// Recebe a carga manual do angular, grava no banco e processa a analise estatistica
	@Transactional
	public QuotationSummaryDTO processQuotationRequest(List<PriceRecord> rawData) {
		if (rawData == null || rawData.isEmpty()) {
			return new QuotationSummaryDTO(List.of(), List.of(), java.math.BigDecimal.ZERO, 0, "MEDIANA");
		}

		// Aplica a esteira estatística diretamente na memória
		calculationService.applyCriticalAnalisys(rawData);
		
		// Salva o lote de registros no MySQL
		priceRecordRepository.saveAll(rawData);
		
		// Extrai o código CATMAT da própria carga para gerar o sumário consolidado
		String catmatCode = rawData.get(0).getCatmatCode();
		return calculationService.generateSummary(catmatCode, rawData);
	}

	public List<PriceRecord> findByCatmat(String catmatCode) {
		return priceRecordRepository.findByCatmatCode(catmatCode);
	}

	// 🎯 MODIFICADO: Alterado de 'void' para retornar o 'QuotationSummaryDTO' calculado na memória
	@Transactional
	public QuotationSummaryDTO syncExternalPrices(String catmatCode) {
		String codigoLimpo = catmatCode.trim();
		
		// 1. LIMPEZA MANDATÓRIA: Apaga qualquer resquício antigo desse CATMAT no MySQL
		List<PriceRecord> antigos = priceRecordRepository.findByCatmatCode(codigoLimpo);
		if (antigos != null && !antigos.isEmpty()) {
			priceRecordRepository.deleteAll(antigos);
			priceRecordRepository.flush(); // Força a exclusão imediata no banco física
		}

		// 2. Busca os novos dados do barramento federal
		List<PriceRecord> externalRecords = comprasGovService.fetchPricesFromGov(codigoLimpo);
		
		if (externalRecords != null && !externalRecords.isEmpty()) {
			// 3. Aplica a análise estatística dos 30% da IN 65/2021 nos dados novos na memória
			calculationService.applyCriticalAnalisys(externalRecords);
			
			// 4. Salva a nova consulta atualizada no banco de dados
			priceRecordRepository.saveAll(externalRecords);
			
			// 5. Retorna o sumário matemático calculado direto sobre os dados vivos da memória
			return calculationService.generateSummary(codigoLimpo, externalRecords);
		}
		
		return new QuotationSummaryDTO(List.of(), List.of(), java.math.BigDecimal.ZERO, 0, "MEDIANA");
	}
}