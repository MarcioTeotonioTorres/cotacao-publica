package com.cotacao.domain.dto;

import java.util.List;

import com.cotacao.domain.model.PriceRecord;

public record QuotationResponseDTO(
		List<PriceRecord> historicalRecords, 
		List<PriceRecord> activeArps
		) {}
