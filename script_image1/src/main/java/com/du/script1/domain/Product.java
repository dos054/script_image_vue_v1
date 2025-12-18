package com.du.script1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    private Long pcode;

    @Column(name = "product_name", length = 500)
    private String productName;

    @Column(length = 500)
    private String url;

    @Column(length = 255)
    private String image;

    @Column(name = "price_min")
    private Integer priceMin;

    @Column(name = "price_max")
    private Integer priceMax;

    @Column(name = "price_balance", columnDefinition = "CLOB")
    private String priceBalance;

    @Column(name = "detail_json", columnDefinition = "CLOB")
    private String detailJson;

    @Transient
    public String getSearchableText() {
        StringBuilder sb = new StringBuilder();
        if (productName != null) sb.append(productName).append(" ");
        if (detailJson != null) sb.append(detailJson).append(" ");
        return sb.toString().trim();
    }
}
