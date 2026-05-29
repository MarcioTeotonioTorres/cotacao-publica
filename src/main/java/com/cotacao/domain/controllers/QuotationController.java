package com.cotacao.domain.controllers;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotacao.domain.dto.QuotationResponseDTO;
import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.service.QuotationOrchestratorService;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins= "http://localHost:4200")
public class QuotationController {	
	
	private final QuotationOrchestratorService orchestratorService; 
	
	@PostMapping("/process")
	public QuotationResponseDTO processRecords(@RequestBody List<PriceRecord> records) {			
		return orchestratorService.processQuotationRequest(records);
	}
	public QuotationController(@Lazy QuotationOrchestratorService orquestratorService) {
		this.orchestratorService = orquestratorService;
	}
		
	 
}
