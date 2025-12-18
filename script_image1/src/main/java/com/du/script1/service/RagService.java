package com.du.script1.service;

import com.du.script1.domain.Product;
import com.du.script1.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final WebClient ollamaWebClient;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ollama.model:llama2}")
    private String modelName;

    /**
     * 두 상품 비교 분석
     */
    public String compareProducts(Long pcode1, Long pcode2) {
        try {
            log.info("상품 비교 요청: {} vs {}", pcode1, pcode2);

            Optional<Product> product1Opt = productRepository.findById(pcode1);
            Optional<Product> product2Opt = productRepository.findById(pcode2);

            if (product1Opt.isEmpty() || product2Opt.isEmpty()) {
                return "선택한 상품을 찾을 수 없습니다.";
            }

            Product p1 = product1Opt.get();
            Product p2 = product2Opt.get();

            // 1단계: JSON 구조화 (Java에서 계산 완료)
            Map<String, Object> comparisonData = buildComparisonJson(p1, p2);
            String jsonData = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(comparisonData);
            
            log.info("구조화된 데이터:\n{}", jsonData);

            // 2단계: LLM에게 자연어로 변환 요청
            String prompt = buildNaturalLanguagePrompt(jsonData);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            var messagesArray = requestBody.putArray("messages");

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messagesArray.add(userMessage);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            log.info("Ollama API 호출 중...");

            String response = ollamaWebClient.post()
                .uri("/api/chat")
                .header("Content-Type", "application/json")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(300))
                .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            String aiResponse = jsonResponse.get("message").get("content").asText();

            // 한자/영어 필터링
            String filteredResponse = filterNonKorean(aiResponse);

            log.info("비교 분석 완료");
            return filteredResponse;

        } catch (Exception e) {
            log.error("상품 비교 중 오류 발생", e);
            return "비교 분석 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * JSON 구조화 - 모든 계산을 Java에서 완료
     */
    private Map<String, Object> buildComparisonJson(Product p1, Product p2) {
        Map<String, Object> result = new LinkedHashMap<>();

        String name1 = p1.getProductName().replace(" : 다나와 가격비교", "");
        String name2 = p2.getProductName().replace(" : 다나와 가격비교", "");

        // 상품 정보
        Map<String, Object> productA = new LinkedHashMap<>();
        productA.put("이름", name1);
        productA.put("최저가", p1.getPriceMin());
        productA.put("최고가", p1.getPriceMax());

        Map<String, Object> productB = new LinkedHashMap<>();
        productB.put("이름", name2);
        productB.put("최저가", p2.getPriceMin());
        productB.put("최고가", p2.getPriceMax());

        result.put("상품A", productA);
        result.put("상품B", productB);

        // 가격 비교 (계산 완료)
        Map<String, Object> priceComparison = new LinkedHashMap<>();
        int priceDiff = p1.getPriceMin() - p2.getPriceMin();
        if (priceDiff < 0) {
            priceComparison.put("더저렴한상품", "상품A");
            priceComparison.put("가격차이", Math.abs(priceDiff));
        } else if (priceDiff > 0) {
            priceComparison.put("더저렴한상품", "상품B");
            priceComparison.put("가격차이", Math.abs(priceDiff));
        } else {
            priceComparison.put("더저렴한상품", "동일");
            priceComparison.put("가격차이", 0);
        }
        result.put("가격비교", priceComparison);

        // 가격 추이 분석 (계산 완료)
        PriceTrendData trend1 = parsePriceTrendData(p1.getPriceBalance());
        PriceTrendData trend2 = parsePriceTrendData(p2.getPriceBalance());

        Map<String, Object> trendAnalysis = new LinkedHashMap<>();
        
        Map<String, Object> trendA = new LinkedHashMap<>();
        trendA.put("3개월변동금액", trend1.diff3Month);
        trendA.put("추세", trend1.diff3Month > 0 ? "올랐음" : (trend1.diff3Month < 0 ? "내렸음" : "변동없음"));
        
        Map<String, Object> trendB = new LinkedHashMap<>();
        trendB.put("3개월변동금액", trend2.diff3Month);
        trendB.put("추세", trend2.diff3Month > 0 ? "올랐음" : (trend2.diff3Month < 0 ? "내렸음" : "변동없음"));

        trendAnalysis.put("상품A추이", trendA);
        trendAnalysis.put("상품B추이", trendB);

        // 안정성 판단
        if (Math.abs(trend1.diff3Month) < Math.abs(trend2.diff3Month)) {
            trendAnalysis.put("더안정적인상품", "상품A");
        } else if (Math.abs(trend1.diff3Month) > Math.abs(trend2.diff3Month)) {
            trendAnalysis.put("더안정적인상품", "상품B");
        } else {
            trendAnalysis.put("더안정적인상품", "비슷함");
        }
        result.put("가격추이분석", trendAnalysis);

        // 종합 추천 (점수 기반 판단 완료)
        Map<String, Object> recommendation = new LinkedHashMap<>();
        int scoreA = 0, scoreB = 0;
        List<String> reasonsA = new ArrayList<>();
        List<String> reasonsB = new ArrayList<>();

        if (p1.getPriceMin() < p2.getPriceMin()) {
            scoreA++;
            reasonsA.add("가격이 더 저렴함");
        } else if (p2.getPriceMin() < p1.getPriceMin()) {
            scoreB++;
            reasonsB.add("가격이 더 저렴함");
        }

        if (Math.abs(trend1.diff3Month) < Math.abs(trend2.diff3Month)) {
            scoreA++;
            reasonsA.add("가격이 안정적임");
        } else if (Math.abs(trend2.diff3Month) < Math.abs(trend1.diff3Month)) {
            scoreB++;
            reasonsB.add("가격이 안정적임");
        }

        if (trend1.diff3Month < trend2.diff3Month) {
            scoreA++;
            reasonsA.add("가격이 내리는 추세임");
        } else if (trend2.diff3Month < trend1.diff3Month) {
            scoreB++;
            reasonsB.add("가격이 내리는 추세임");
        }

        if (scoreA > scoreB) {
            recommendation.put("추천상품", "상품A");
            recommendation.put("추천이유", reasonsA);
        } else if (scoreB > scoreA) {
            recommendation.put("추천상품", "상품B");
            recommendation.put("추천이유", reasonsB);
        } else {
            recommendation.put("추천상품", "둘다비슷함");
            recommendation.put("추천이유", List.of("조건이 비슷하므로 개인 취향에 따라 선택"));
        }
        result.put("종합추천", recommendation);

        return result;
    }

    /**
     * LLM에게 자연어 변환 요청하는 프롬프트
     */
    private String buildNaturalLanguagePrompt(String jsonData) {
        return String.format("""
아래 JSON 데이터를 자연스러운 한국어 문장으로 바꿔주세요.

[절대 규칙]
1. JSON에 있는 숫자와 판단 결과를 절대 변경하지 마세요
2. 새로운 정보를 추가하지 마세요
3. 한글, 숫자, 쉼표, 마침표만 사용하세요
4. 한자 사용 금지 (예: 上昇, 下落, 比較 금지)
5. 영어 사용 금지
6. 일본어 사용 금지

[JSON 데이터]
%s

[출력 형식]
## 가격 비교
(상품A와 상품B의 가격을 비교하는 문장)

## 가격 추이 분석  
(3개월간 가격 변동을 설명하는 문장)

## 종합 추천
(추천 상품과 이유를 설명하는 문장)

먼저 영어로 작성한 후 한국어로 번역해서 최종 결과만 보여주세요.
영어 원문은 출력하지 마세요.
""", jsonData);
    }

    /**
     * 한자/영어/일본어 필터링
     */
    private String filterNonKorean(String text) {
        if (text == null) return "";
        
        // 한자 → 한글 치환
        text = text.replace("上昇", "올랐음");
        text = text.replace("下落", "내렸음");
        text.replace("比較", "비교");
        text = text.replace("推薦", "추천");
        text = text.replace("價格", "가격");
        text = text.replace("商品", "상품");
        text = text.replace("分析", "분석");
        text = text.replace("綜合", "종합");
        text = text.replace("安定", "안정");
        
        // 일본어 제거
        text = text.replaceAll("[\\u3040-\\u309F\\u30A0-\\u30FF]", "");
        
        return text;
    }

    /**
     * 가격 추이 데이터 파싱
     */
    private PriceTrendData parsePriceTrendData(String priceBalanceJson) {
        PriceTrendData data = new PriceTrendData();
        
        if (priceBalanceJson == null || priceBalanceJson.isEmpty()) {
            return data;
        }

        try {
            Map<String, List<Map<String, Object>>> priceData = objectMapper.readValue(
                priceBalanceJson, 
                new TypeReference<Map<String, List<Map<String, Object>>>>() {}
            );

            if (priceData.containsKey("3")) {
                List<Map<String, Object>> list = priceData.get("3");
                if (!list.isEmpty()) {
                    data.firstPrice3Month = ((Number) list.get(0).get("price")).intValue();
                    data.lastPrice3Month = ((Number) list.get(list.size() - 1).get("price")).intValue();
                    data.diff3Month = data.lastPrice3Month - data.firstPrice3Month;
                }
            }

            if (priceData.containsKey("1")) {
                List<Map<String, Object>> list = priceData.get("1");
                if (!list.isEmpty()) {
                    data.firstPrice1Month = ((Number) list.get(0).get("price")).intValue();
                    data.lastPrice1Month = ((Number) list.get(list.size() - 1).get("price")).intValue();
                    data.diff1Month = data.lastPrice1Month - data.firstPrice1Month;
                }
            }

        } catch (Exception e) {
            log.warn("가격 추이 파싱 실패: {}", e.getMessage());
        }

        return data;
    }

    private static class PriceTrendData {
        int firstPrice1Month = 0;
        int lastPrice1Month = 0;
        int diff1Month = 0;
        int firstPrice3Month = 0;
        int lastPrice3Month = 0;
        int diff3Month = 0;
    }

    /**
     * 검색 결과를 JSON 구조화
     */
    public Map<String, Object> searchAndStructure(String question) {
        List<Product> products = searchRelevantProducts(question);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", question);
        result.put("totalFound", products.size());

        List<Map<String, Object>> productList = products.stream()
            .limit(10)
            .map(p -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("pcode", p.getPcode());
                item.put("productName", p.getProductName());
                item.put("priceMin", p.getPriceMin());
                item.put("priceMax", p.getPriceMax());
                item.put("url", p.getUrl());
                item.put("image", p.getImage());
                return item;
            })
            .collect(Collectors.toList());

        result.put("products", productList);

        return result;
    }

    private List<Product> searchRelevantProducts(String question) {
        String[] keywords = question.split("\\s+");

        return productRepository.findAll().stream()
            .filter(product -> {
                String searchText = product.getSearchableText().toLowerCase();
                for (String keyword : keywords) {
                    if (searchText.contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
                return false;
            })
            .limit(10)
            .collect(Collectors.toList());
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.searchByMultipleFields(keyword);
    }
}
