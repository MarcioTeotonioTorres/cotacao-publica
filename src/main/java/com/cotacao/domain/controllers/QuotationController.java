package com.cotacao.domain.controllers;

import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.cotacao.domain.dto.QuotationSummaryDTO;
import com.cotacao.domain.model.PriceRecord;
import com.cotacao.domain.service.ExportReportService;
import com.cotacao.domain.service.PriceCalculationService;
import com.cotacao.domain.service.QuotationOrchestratorService;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "http://localhost:4200")
public class QuotationController {

	private final QuotationOrchestratorService orchestratorService;
	private final PriceCalculationService calculationService;
	private final ExportReportService exportReportService;

	// 🎯 AJUSTE 1: Injeção correta e atribuição das variáveis gerenciadas pelo Spring Boot
	public QuotationController(@Lazy QuotationOrchestratorService orchestratorService, 
							   PriceCalculationService calculationService, ExportReportService exportReportService) {
		this.orchestratorService = orchestratorService;
		this.calculationService = calculationService;
		this.exportReportService = exportReportService;
	}
	
	// Recebe a lista crua do Angular, processa, salva e retorna o sumário estatístico combinado
	@PostMapping("/process")
	public ResponseEntity<QuotationSummaryDTO> processRecords(@RequestBody List<PriceRecord> records) {			
		QuotationSummaryDTO response = orchestratorService.processQuotationRequest(records);
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/catmat/{code}")
	public ResponseEntity<QuotationSummaryDTO> getQuotationByCatmatCode(@PathVariable String code) {
		String cleanCode = code.trim();
		List<PriceRecord> recordsFromDb = orchestratorService.findByCatmat(cleanCode); 
		
		if (recordsFromDb.isEmpty()) {
			return ResponseEntity.ok(new QuotationSummaryDTO(List.of(), List.of(), java.math.BigDecimal.ZERO, 0, "MEDIANA"));
		}
		
		// Gera o sumário completo e calcula a mediana com base nos dados existentes no MySQL
		QuotationSummaryDTO summary = calculationService.generateSummary(cleanCode, recordsFromDb);
		return ResponseEntity.ok(summary);
	}

	@PostMapping("/catmat/{code}/sync")
	public ResponseEntity<QuotationSummaryDTO> syncAndGetQuotation(@PathVariable String code) {
		String cleanCode = code.trim();
		
		// 🎯 AJUSTE 2: Executa a nova esteira unificada e recupera o DTO com o preço calculado direto da memória
		QuotationSummaryDTO summary = orchestratorService.syncExternalPrices(cleanCode);
		
		return ResponseEntity.ok(summary);  
	}
	@PostMapping("/catmat/{code}/report")
	public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadReport(
			@PathVariable String code, 
			@RequestBody com.cotacao.domain.dto.ReportRequestDTO metaData) {
		
		String cleanCode = code.trim();
		// 1. Busca os registros saneados que estão salvos no MySQL
		List<PriceRecord> records = orchestratorService.findByCatmat(cleanCode);
		
		// 2. Consolida os dados no DTO matemático unificado
		com.cotacao.domain.dto.QuotationSummaryDTO summary = calculationService.generateSummary(cleanCode, records);
		
		// 3. Dispara o motor de desenho do OpenPDF passando os dados e os metadados
		java.io.ByteArrayInputStream pdfStream = exportReportService.generatePriceResearchPdf(cleanCode, summary, metaData);
		
		org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=Price_Research_Report_" + cleanCode + ".pdf");
		
		return ResponseEntity.ok()
				.headers(headers)
				.contentType(org.springframework.http.MediaType.APPLICATION_PDF)
				.body(new org.springframework.core.io.InputStreamResource(pdfStream));
	}
}