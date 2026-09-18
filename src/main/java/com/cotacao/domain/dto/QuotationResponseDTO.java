package com.cotacao.domain.dto;

import java.util.List;

import com.cotacao.domain.model.PriceRecord;
import com.fasterxml.jackson.annotation.JsonProperty;



public record QuotationResponseDTO(
		@JsonProperty
		List<PriceRecord> historicalRecords, 
		@JsonProperty
		List<PriceRecord> activeArps
		) {}
