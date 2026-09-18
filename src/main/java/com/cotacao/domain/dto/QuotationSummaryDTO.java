package com.cotacao.domain.dto;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.cotacao.domain.model.PriceRecord;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuotationSummaryDTO {

    private List<PriceRecord> historicalRecords;
    private List<PriceRecord> activeArps;
    
    // Removemos as anotações complexas do Lombok daqui
    private BigDecimal precoReferenciaCombinado;
    private int totalItensValidos;
    private String metodoCalculo; 

    // 🎯 ESCREVENDO OS GETTERS MANUAIS PARA GARANTIR O MAPEAMENTO DO JACKSON
    
    @JsonProperty("combinedReferencePrice")
    public BigDecimal getPrecoReferenciaCombinado() {
        return this.precoReferenciaCombinado;
    }

    @JsonProperty("totalValidItems")
    public int getTotalItensValidos() {
        return this.totalItensValidos;
    }

    @JsonProperty("calculationMethod")
    public String getMetodoCalculo() {
        return this.metodoCalculo;
    }
}