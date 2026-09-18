package com.cotacao.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReportRequestDTO {

    private String administrativeProcess; // Número do Processo (Ex: CI Nº 058/2026)
    private String requestingDepartment;  // Diretoria de Atenção Especializada
    private String responsibleName;       // Nome do Fiscal / Técnico (Ex: Pedro Emanuel Silva)
    private String responsibleRole;       // Cargo (Ex: Gerente de Compras)

   
}