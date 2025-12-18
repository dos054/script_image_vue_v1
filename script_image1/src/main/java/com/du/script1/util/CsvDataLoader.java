package com.du.script1.util;

import com.du.script1.domain.Product;
import com.du.script1.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsvDataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        String csvFile = "danawa_유모차_output_final_cleaned_img_modified.csv";
        
        try {
            ClassPathResource resource = new ClassPathResource(csvFile);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );

            List<Product> products = new ArrayList<>();
            String line;
            boolean isFirst = true;

            while ((line = reader.readLine()) != null) {
                // 헤더 스킵
                if (isFirst) {
                    isFirst = false;
                    continue;
                }

                try {
                    Product product = parseCsvLine(line);
                    if (product != null) {
                        products.add(product);
                    }
                } catch (Exception e) {
                    log.warn("CSV 파싱 실패: {}", e.getMessage());
                }
            }

            reader.close();
            productRepository.saveAll(products);
            log.info("CSV 데이터 로드 완료: {}개 상품", products.size());

        } catch (Exception e) {
            log.error("CSV 파일 로드 실패: {}", e.getMessage());
        }
    }

    private Product parseCsvLine(String line) {
        // CSV 파싱 (큰따옴표 내부 쉼표 처리)
        List<String> fields = parseCsvFields(line);
        
        if (fields.size() < 7) {
            return null;
        }

        try {
            return Product.builder()
                .pcode(Long.parseLong(fields.get(0).trim()))
                .productName(fields.get(1).trim())
                .url(fields.get(2).trim())
                .image(fields.get(3).trim())
                .priceMin(parseInteger(fields.get(4)))
                .priceMax(parseInteger(fields.get(5)))
                .priceBalance(fields.get(6).trim())
                .detailJson(fields.size() > 7 ? fields.get(7).trim() : null)
                .build();
        } catch (Exception e) {
            log.warn("상품 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 이스케이프된 따옴표 ""
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields;
    }

    private Integer parseInteger(String value) {
        try {
            String cleaned = value.trim().replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }
}
