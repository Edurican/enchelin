package com.edurican.enchelinbe.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KakaoApiResponseDto {

    @JsonProperty("meta")
    private Meta meta;

    private List<Document> documents;

    @Getter
    @NoArgsConstructor
    public static class Meta {
        @JsonProperty("is_end")
        private boolean isEnd;

        @JsonProperty("pageable_count")
        private int pageableCount;
    }

    @Getter
    @NoArgsConstructor
    public static class Document {
        @JsonProperty("id")
        private String id;

        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("road_address_name")
        private String roadAddressName;

        @JsonProperty("address_name")
        private String addressName; // (도로명 없을 때 대비)

        @JsonProperty("phone")
        private String phone;

        @JsonProperty("x")
        private String x; // 경도

        @JsonProperty("y")
        private String y; // 위도

        @JsonProperty("place_url")
        private String placeUrl;
    }
}
