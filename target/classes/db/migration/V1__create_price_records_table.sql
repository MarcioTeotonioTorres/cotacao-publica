CREATE TABLE price_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    catmat_code VARCHAR(20) NOT NULL,
    unit_price DECIMAL(19,4) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    supplier_cnpj VARCHAR(18),
    research_date DATETIME NOT NULL,
    evidence_url TEXT,
    record_type VARCHAR(50) NOT NULL, -- 'HISTORICAL_PURCHASE' ou 'ACTIVE_ARP'
    
    -- Campos específicos para controle de Atas de Registro de Preços
    available_quantity INT,
    validity_date DATE,
    
    -- Campos exigidos para a Análise Crítica da IN 65/2021
    excluded_by_critical_analysis BOOLEAN DEFAULT FALSE,
    exclusion_justification TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;