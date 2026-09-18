package com.cotacao.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.cotacao.domain.dto.QuotationSummaryDTO;
import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.model.RecordType;

@Service
public class PriceCalculationService {

	// fatores de multiplicação para margem de 30%
	private static final BigDecimal UPPER_LIMIT = new BigDecimal("1.30");
	private static final BigDecimal LOWER_LIMIT = new BigDecimal("0.70");

	public BigDecimal calculateMedian(List<BigDecimal> values) {
		if (values == null || values.isEmpty()) {
			return BigDecimal.ZERO;
		}

		List<BigDecimal> sorted = values.stream().filter(Objects::nonNull).sorted().toList();

		int size = sorted.size();
		int mid = size / 2;

		if (size % 2 == 0) {
			return sorted.get(mid - 1).add(sorted.get(mid)).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
		}

		return sorted.get(mid);
	}

	public void applyCriticalAnalisys(List<PriceRecord> records) {
		if ((records == null) || records.isEmpty()) return; 
		
		// 🎯 CORREÇÃO CRUCIAL: Separa apenas os registros de compras históricas para calcular a mediana real de mercado
		List<PriceRecord> historicalRecords = records.stream()
				.filter(r -> r.getRecordType() == RecordType.HISTORICAL_PURCHASE)
				.toList();

		// Se não houver históricos, as ARPs não devem ser descartadas por variação de 30%
		if (historicalRecords.isEmpty()) {
			records.forEach(r -> {
				if (r.getRecordType() == RecordType.ACTIVE_ARP) {
					r.setExcludedByCriticalAnalyses(false);
					r.setExclusionJustification(null);
				}
			});
			return;
		}
		
		// Extrai apenas os valores não nulos dos HISTÓRICOS
		List<BigDecimal> validPrices = historicalRecords.stream()
				.map(PriceRecord::getUnitPrice)
				.filter(Objects::nonNull)
				.toList();
			
		if (validPrices.isEmpty()) return;
		
		// Se a amostra de históricos for muito pequena, preserva para avaliação manual
		if (validPrices.size() < 3) {
			records.forEach(record -> {
				if (record.getUnitPrice() == null) {
					record.setExcludedByCriticalAnalyses(true);
					record.setExclusionJustification("Preço unitário não informado na origem");
				} else {
					record.setExcludedByCriticalAnalyses(false);
					record.setExclusionJustification("Amostra reduzida (" + validPrices.size() + " itens). Validação manual obrigatória.");
				}
			});
			return;
		}
		
		// Mediana calculada estritamente sobre as compras reais, sem distorção das ARPs
		BigDecimal median = calculateMedian(validPrices);
		BigDecimal maxAcceptable = median.multiply(UPPER_LIMIT);
		BigDecimal minAcceptable = median.multiply(LOWER_LIMIT);
		
		// Aplica a validação item por item na lista completa
		records.forEach(record -> {
			BigDecimal price = record.getUnitPrice();
			
			if (price == null) {
				record.setExcludedByCriticalAnalyses(true);
				record.setExclusionJustification("Preço unitário não informado na origem");
			} 
			// Se for compra histórica, passa pelo crivo dos 30% da IN 65/2021
			else if (record.getRecordType() == RecordType.HISTORICAL_PURCHASE) {
				if (price.compareTo(maxAcceptable) > 0 || price.compareTo(minAcceptable) < 0) {
					record.setExcludedByCriticalAnalyses(true);
					record.setExclusionJustification(String.format("Excluído: Variação superior a 30%% (Mediana: R$ %.2f)", median));
				} else {
					record.setExcludedByCriticalAnalyses(false);
					record.setExclusionJustification(null);
				}
			} 
			// Se for ARP ativa, ela entra direto na amostragem limpa como fonte complementar válida
			else if (record.getRecordType() == RecordType.ACTIVE_ARP) {
				record.setExcludedByCriticalAnalyses(false);
				record.setExclusionJustification(null);
			}
		});
	}
	
	public QuotationSummaryDTO generateSummary(String catmatCode, List<PriceRecord> records) {
		if (records == null || records.isEmpty()) {
			return new QuotationSummaryDTO(List.of(), List.of(), BigDecimal.ZERO, 0, "MEDIANA");
		}
		
		// 1. Separa dinamicamente a lista unificada do banco para alimentar o DTO
		List<PriceRecord> historicalRecords = records.stream()
				.filter(r -> r.getRecordType() == RecordType.HISTORICAL_PURCHASE)
				.toList();
				
		List<PriceRecord> activeArps = records.stream()
				.filter(r -> r.getRecordType() == RecordType.ACTIVE_ARP)
				.toList();
		
		// 🎯 CORREÇÃO CIRÚRGICA: Filtra comparando o objeto de forma segura. 
		// Se for null ou explicitamente false, significa que o preço FOI ACEITO!
		List<BigDecimal> acceptedPrices = records.stream()
				.filter(r -> r.getExcludedByCriticalAnalyses() == null || Boolean.FALSE.equals(r.getExcludedByCriticalAnalyses()))
				.map(PriceRecord::getUnitPrice)
				.filter(Objects::nonNull)
				.toList();
		
		// 3. Calcula a mediana real combinada da amostra saudável
		BigDecimal precoReferenciaCombinado = calculateMedian(acceptedPrices);
		int totalItensValidos = acceptedPrices.size();
		
		// 4. Retorna a instância perfeita com os tipos correspondentes do seu construtor
		return new QuotationSummaryDTO(
				historicalRecords,
				activeArps,
				precoReferenciaCombinado,
				totalItensValidos,
				"MEDIANA"
		);
	} 
}