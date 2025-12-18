package com.du.script1.repository;

import com.du.script1.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 제품명으로 검색
     */
    List<Product> findByProductNameContaining(String name);

    /**
     * 가격 범위로 검색
     */
    List<Product> findByPriceMinBetween(Integer minPrice, Integer maxPrice);

    /**
     * 키워드 검색 (제품명)
     */
    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:keyword%")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 통합 검색
     */
    @Query("SELECT DISTINCT p FROM Product p WHERE p.productName LIKE %:keyword%")
    List<Product> searchByMultipleFields(@Param("keyword") String keyword);
}
