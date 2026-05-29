package com.cotacao.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "price_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PriceRecord {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "catmat_code", nullable = false, length = 20)
	private String catmatCode;
	
	@Column(name = "unit_price", precision = 19, scale = 4, nullable = false)
	private BigDecimal unitPrice;
	
	@Column(name = "source_name", nullable = false)
	private String sourceName;
	
	@Column(name = "suplier_cnpj", length = 18)
	private String suplierCnpj;

	@Column(name = "research_date", nullable = false)
	private LocalDateTime researchDate;
	
	@Column(name = "evidence_url", columnDefinition = "TEXT")
	private String evidenceUrl;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "record_type", nullable = false, length = 50)
	private RecordType recordType;
	
	@Column(name = "avaliableQuantity")
	private Integer avaliableQuantity;
	
	@Column(name = "validity_date")
	private LocalDate validityDate;
	
	
	@Column(name = "excluded_by_critical_analyses")
	private Boolean excludedByCriticalAnalysis;
	
	@Column(name = "exclusion_justification", columnDefinition = "TEXT")
	private String exclusionJustification;
	
	
}
