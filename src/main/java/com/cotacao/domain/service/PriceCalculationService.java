package com.cotacao.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.cotacao.domain.model.PriceRecord;

@Service
public class PriceCalculationService {
	
	//fatores de multiplicação para margem de 30%
	private static final BigDecimal UPPER_LIMIT = new BigDecimal("1.30");
	private static final BigDecimal LOWER_LIMIT = new BigDecimal("0.70");
	
	public BigDecimal calculateMedian(List<BigDecimal> values) {
		
		if(values == null || values.isEmpty()){
			return BigDecimal.ZERO;
		}
		
		List<BigDecimal> sorted = values.stream().filter(Objects::nonNull).sorted().toList();
		
		int size = sorted.size();
		int mid = size / 2;
		
		if(size % 2 == 0) {
			return sorted.get(mid-1).add(sorted.get(mid)).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
		}
		
		return sorted.get(mid); 
	}
	
	public void applyCriticalAnalisys(List<PriceRecord> records) {
		
	
		
		//Extrai apenas os valores não
		List<BigDecimal> validPrices = records.stream()
				.map(PriceRecord::getUnitPrice)
				.filter(Objects::nonNull)
				.toList();
		
		if(validPrices.isEmpty()) return;
		
		if(validPrices.size() < 3) {
			records.forEach(record -> {
				if(record.getUnitPrice() == null) {
					record.setExcludedByCriticalAnalysis(true);
					record.setExclusionJustification("Preço unitário não informado na origem");
				}else {
					record.setExcludedByCriticalAnalysis(false);
					record.setExclusionJustification("Amostra reduzida (" + validPrices.size() + " itens). Validação manual obrigatória pelo Fiscal.");
				}
			});
			return;
		}
		
		BigDecimal median = calculateMedian(validPrices);
		BigDecimal maxAcceptable = median.multiply(UPPER_LIMIT);
		BigDecimal minAcceptable = median.multiply(LOWER_LIMIT);
		
		//applica o filtro por ausência de preço unitário, max. e min., item por item		
		records.forEach(record -> {
			BigDecimal price = record.getUnitPrice();
			
			if(price == null) {
				record.setExcludedByCriticalAnalysis(true);
				record.setExclusionJustification("Preço unitário não informado na origem");
			}else if(price.compareTo(maxAcceptable) > 0 || price.compareTo(minAcceptable) < 0){
				record.setExcludedByCriticalAnalysis(true);
				record.setExclusionJustification(String.format("Excluído: Variação superior a 30%% (Mediana: R$ %.2f)", median));
								
			}else {
				record.setExcludedByCriticalAnalysis(false);
				record.setExclusionJustification(null);
			}		
		
	});
		
	}
} 