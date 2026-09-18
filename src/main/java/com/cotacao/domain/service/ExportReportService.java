package com.cotacao.domain.service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import com.cotacao.domain.dto.ReportRequestDTO;
import com.cotacao.domain.dto.QuotationSummaryDTO;
import com.cotacao.domain.model.PriceRecord;

@Service
public class ExportReportService {

    public ByteArrayInputStream generatePriceResearchPdf(String catmatCode, QuotationSummaryDTO summary, ReportRequestDTO metaData) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Configuração de Fontes Padronizadas (Identidade Oficial)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(0, 86, 179));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);

            // 2. Cabeçalho Institucional do Município
            Paragraph header = new Paragraph("ESTADO DE PERNAMBUCO\nPREFEITURA MUNICIPAL DE IPOJUCA", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(4);
            document.add(header);

            Paragraph subtitle = new Paragraph("SECRETARIA MUNICIPAL DE SAÚDE\nMÓDULO DE SANEAMENTO E APURAÇÃO DE PREÇOS (LEI Nº 14.133/21)", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // 3. Quadro de Metadados do Processo Licitatório
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);
            
            metaTable.addCell(new PdfPCell(new Phrase("Processo Administrativo: " + metaData.getAdministrativeProcess(), normalFont)));
            metaTable.addCell(new PdfPCell(new Phrase("Setor Requisitante: " + metaData.getRequestingDepartment(), normalFont)));
            metaTable.addCell(new PdfPCell(new Phrase("Código CATMAT do Item: " + catmatCode, normalFont)));
            metaTable.addCell(new PdfPCell(new Phrase("Data de Emissão: " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont)));
            document.add(metaTable);

            // 4. Seção 1: Relatório Técnico de Pesquisa
            Paragraph sec1 = new Paragraph("1. RELATÓRIO TÉCNICO DE PROSPECÇÃO DE MERCADO", sectionFont);
            sec1.setSpacingBefore(10);
            sec1.setSpacingAfter(10);
            document.add(sec1);

            Paragraph textIntro = new Paragraph("Em cumprimento ao disposto na Instrução Normativa regulamentar federal nº 65/2021 e no art. 23 da Lei Federal nº 14.133/21, realizou-se a extração de preços praticados no mercado público via barramento de Dados Abertos do Compras.gov.br e Portal Nacional de Contratações Públicas (PNCP), obtendo-se o seguinte resultado apurado:", normalFont);
            textIntro.setSpacingAfter(15);
            document.add(textIntro);

            // 5. Tabela de Preços Saneados (Amostra Combinada)
            PdfPTable dataTable = new PdfPTable(4);
            dataTable.setWidthPercentage(100);
            dataTable.setWidths(new float[]{1.5f, 4.5f, 2f, 2f});
            dataTable.setSpacingAfter(15);

            // Cabeçalho da Tabela
            String[] headers = {"Tipo de Origem", "Órgão Público / Identificação do Registro", "Preço Unitário", "Saneamento Estatístico"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(new Color(233, 236, 239));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                dataTable.addCell(cell);
            }

            // Alimenta com os Registros Históricos de Compras
            if (summary.getHistoricalRecords() != null) {
                for (PriceRecord r : summary.getHistoricalRecords()) {
                    dataTable.addCell(new PdfPCell(new Phrase("Licitação", normalFont)));
                    dataTable.addCell(new PdfPCell(new Phrase(r.getSourceName(), normalFont)));
                    
                    PdfPCell priceCell = new PdfPCell(new Phrase(String.format("R$ %.2f", r.getUnitPrice()), normalFont));
                    priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    dataTable.addCell(priceCell);
                    
                    // 🎯 CORREÇÃO: Utilizando o getter do Lombok para Boolean objeto (get e checagem de null seguro)
                    String status = (r.getExcludedByCriticalAnalyses() != null && r.getExcludedByCriticalAnalyses()) 
                                    ? "REJEITADO (Outlier)" : "ACEITO";
                    dataTable.addCell(new PdfPCell(new Phrase(status, normalFont)));
                }
            }

            // Alimenta com as Atas de Registro de Preços (ARP)
            if (summary.getActiveArps() != null) {
                for (PriceRecord r : summary.getActiveArps()) {
                    dataTable.addCell(new PdfPCell(new Phrase("ARP", normalFont)));
                    dataTable.addCell(new PdfPCell(new Phrase(r.getSourceName(), normalFont)));
                    
                    PdfPCell priceCell = new PdfPCell(new Phrase(String.format("R$ %.2f", r.getUnitPrice()), normalFont));
                    priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    dataTable.addCell(priceCell);
                    
                    // 🎯 CORREÇÃO: Utilizando o getter do Lombok para Boolean objeto
                    String status = (r.getExcludedByCriticalAnalyses() != null && r.getExcludedByCriticalAnalyses()) 
                                    ? "REJEITADO (Outlier)" : "ACEITO";
                    dataTable.addCell(new PdfPCell(new Phrase(status, normalFont)));
                }
            }
            document.add(dataTable);

            // 6. Bloco do Resultado Estatístico Final (Preço de Referência)
            PdfPTable resultTable = new PdfPTable(2);
            resultTable.setWidthPercentage(100);
            resultTable.setSpacingAfter(25);
            
            PdfPCell cellLabel = new PdfPCell(new Phrase("PREÇO DE REFERÊNCIA ESTIMADO (MEDIANA COMBINADA):", boldFont));
            cellLabel.setBackgroundColor(new Color(222, 226, 230));
            cellLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellLabel.setPadding(8);
            
            // 🎯 CORREÇÃO: Utilizando o método correto getPrecoReferenciaCombinado() do seu DTO
            PdfPCell cellValue = new PdfPCell(new Phrase(String.format("R$ %.2f", summary.getPrecoReferenciaCombinado()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(0, 86, 179))));
            cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellValue.setPadding(8);
            
            resultTable.addCell(cellLabel);
            resultTable.addCell(cellValue);
            document.add(resultTable);

            // 7. Seção 2: Declaração de Compatibilidade Locorregional
            Paragraph sec2 = new Paragraph("2. DECLARAÇÃO DE COMPATIBILIDADE LOCORREGIONAL", sectionFont);
            sec2.setSpacingBefore(10);
            sec2.setSpacingAfter(10);
            document.add(sec2);

            // 🎯 CORREÇÃO: Utilizando o método correto getPrecoReferenciaCombinado() do seu DTO
            String termoDeclaracao = String.format(
                "Eu, %s, na qualidade de %s, declaro formalmente para os devidos fins de instrução processual que o preço de referência estimado de R$ %.2f para o item de código CATMAT %s foi obtido mediante metodologia matemática e estatística regular de saneamento de mercado público baseado nas contratações executadas pela Administração. Atesto que o valor final apurado reflete a realidade locorregional e mostra-se compatível com as especificações contidas no Termo de Referência deste órgão, em estrita obediência ao art. 23 da Lei nº 14.133/21.",
                metaData.getResponsibleName(), metaData.getResponsibleRole(), summary.getPrecoReferenciaCombinado(), catmatCode
            );
            Paragraph textDeclaracao = new Paragraph(termoDeclaracao, normalFont);
            textDeclaracao.setSpacingAfter(45);
            textDeclaracao.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(textDeclaracao);

            // 8. Rodapé de Assinatura
            Paragraph lineAssinatura = new Paragraph("____________________________________________________\n" 
                + metaData.getResponsibleName().toUpperCase() + "\n" + metaData.getResponsibleRole(), normalFont);
            lineAssinatura.setAlignment(Element.ALIGN_CENTER);
            document.add(lineAssinatura);

            // 9. Informações de Rodapé Tecnológico
            Paragraph techInfo = new Paragraph("\n\nDocumento gerado automaticamente pelo Módulo de Cotação Pública e Saneamento Estatístico.\nPrefeitura Municipal - Secretaria de Saúde.", italicFont);
            techInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(techInfo);

            document.close();
        } catch (DocumentException e) {
            System.err.println("Erro crítico ao estruturar documento PDF com OpenPDF: " + e.getMessage());
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}