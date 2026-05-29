package com.cotacao.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cotacao.domain.dto.QuotationResponseDTO;
import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.model.RecordType;
import com.cotacao.domain.repository.PriceRecordRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class QuotationOrchestratorService {
	
	private final PriceCalculationService calculationService;
	private final PriceRecordRepository priceRecordRepository;

	public QuotationResponseDTO processQuotationRequest(List<PriceRecord> rawData) {
		// 1. Separa o histórico de compras das Atas de Registro de Preços (ARPs) vigentes
		List<PriceRecord> historicalRecords = rawData.stream()
				.filter(record -> record.getRecordType() == RecordType.HISTORICAL_PURCHASE).toList();
		List<PriceRecord> activeArps = rawData.stream()
				.filter(record -> record.getRecordType() == RecordType.ACTIVE_ARP).toList();
		
		// 2. Aplica o saneamento e a regra dos 30% (IN 65/2021) APENAS no histórico de compras
		calculationService.applyCriticalAnalisys(historicalRecords);
		priceRecordRepository.saveAll(rawData);
		return new QuotationResponseDTO(historicalRecords, activeArps);
	}

}
