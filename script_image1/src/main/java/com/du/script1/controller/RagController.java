package com.du.script1.controller;

import com.du.script1.domain.Product;
import com.du.script1.repository.ProductRepository;
import com.du.script1.service.RagService;
import com.du.script1.service.ImageSimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final ProductRepository productRepository;
    private final ImageSimilarityService imageSimilarityService;

    @GetMapping("/api/products")
    @ResponseBody
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    /**
     * 검색 결과 JSON API
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam String query) {
        log.info("검색 API: {}", query);
        Map<String, Object> result = ragService.searchAndStructure(query);
        return ResponseEntity.ok(result);
    }

    /**
     * 두 상품 비교 API
     */
    @PostMapping("/api/compare")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> compareProducts(@RequestBody Map<String, Long> request) {
        Long pcode1 = request.get("pcode1");
        Long pcode2 = request.get("pcode2");

        log.info("상품 비교 요청: {} vs {}", pcode1, pcode2);

        String result = ragService.compareProducts(pcode1, pcode2);

        return ResponseEntity.ok(Map.of(
            "pcode1", pcode1,
            "pcode2", pcode2,
            "analysis", result
        ));
    }

    /**
     * 유사 이미지 검색 API
     */
    @GetMapping("/api/similar-images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> searchSimilarImages(
            @RequestParam String pcode,
            @RequestParam(defaultValue = "10") int top) {
        
        log.info("유사 이미지 검색 요청: pcode={}, top={}", pcode, top);
        Map<String, Object> result = imageSimilarityService.searchSimilarImages(pcode, top);
        return ResponseEntity.ok(result);
    }
}
