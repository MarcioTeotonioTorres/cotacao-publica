package com.cotacao.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cotacao.domain.model.PriceRecord;

@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long>{
		
	List<PriceRecord> findByCatmatCode(String catmatCode);
}		
