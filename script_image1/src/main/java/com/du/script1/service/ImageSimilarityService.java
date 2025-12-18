package com.du.script1.service;

import com.du.script1.domain.Product;
import com.du.script1.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageSimilarityService {

    private final ProductRepository productRepository;

    @Value("${python.path:python}")
    private String pythonPath;

    @Value("${similarity.script.path:C:/_dev5/projects_2v/script_image1/scripts/similarity_search.py}")
    private String scriptPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> searchSimilarImages(String productId, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            log.info("유사 이미지 검색 시작: productId={}, topN={}", productId, topN);
            
            ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                scriptPath,
                "--id", productId,
                "--top", String.valueOf(topN)
            );
            
            pb.redirectErrorStream(false);
            Process process = pb.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            if (errorOutput.length() > 0) {
                log.info("Python stderr: {}", errorOutput);
            }
            
            int exitCode = process.waitFor();
            log.info("Python 스크립트 종료 코드: {}", exitCode);
            
            String jsonOutput = output.toString().trim();
            log.info("Python stdout: {}", jsonOutput);
            
            int jsonStart = jsonOutput.indexOf("{");
            if (jsonStart > 0) {
                jsonOutput = jsonOutput.substring(jsonStart);
            }
            
            if (jsonOutput.isEmpty() || !jsonOutput.startsWith("{")) {
                result.put("success", false);
                result.put("error", "Python 스크립트 출력 오류");
                return result;
            }
            
            JsonNode jsonNode = objectMapper.readTree(jsonOutput);
            
            if (jsonNode.has("success") && jsonNode.get("success").asBoolean()) {
                result.put("success", true);
                result.put("queryProductId", productId);
                
                List<Map<String, Object>> similarImages = new ArrayList<>();
                JsonNode imagesNode = jsonNode.get("similar_images");
                
                if (imagesNode != null && imagesNode.isArray()) {
                    for (JsonNode img : imagesNode) {
                        double similarity = img.get("similarity").asDouble();
                        // 100% 유사도(자기 자신) 제외
                        if (similarity >= 0.999) {
                            continue;
                        }
                        
                        Map<String, Object> imageInfo = new LinkedHashMap<>();
                        String pId = img.get("product_id").asText();
                        imageInfo.put("productId", pId);
                        imageInfo.put("imageName", img.get("image_name").asText());
                        imageInfo.put("similarity", similarity);
                        
                        // DB에서 상품 정보 조회
                        try {
                            Long pcode = Long.parseLong(pId.replace(".jpg", ""));
                            Optional<Product> productOpt = productRepository.findById(pcode);
                            if (productOpt.isPresent()) {
                                Product product = productOpt.get();
                                imageInfo.put("productName", product.getProductName());
                                imageInfo.put("priceMin", product.getPriceMin());
                                imageInfo.put("priceMax", product.getPriceMax());
                            }
                        } catch (Exception e) {
                            log.warn("상품 정보 조회 실패: {}", pId);
                        }
                        
                        similarImages.add(imageInfo);
                    }
                }
                
                result.put("similarImages", similarImages);
                result.put("totalResults", similarImages.size());
            } else {
                result.put("success", false);
                result.put("error", jsonNode.has("error") ? jsonNode.get("error").asText() : "Unknown error");
            }
            
        } catch (Exception e) {
            log.error("유사 이미지 검색 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        
        return result;
    }
}
