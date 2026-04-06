package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.common.exception.BusinessException;
import com.edurican.enchelinbe.common.exception.ErrorCode;
import com.edurican.enchelinbe.dto.KakaoApiResponseDto;
import com.edurican.enchelinbe.dto.RestaurantResponse;
import com.edurican.enchelinbe.dto.RestaurantSearchResponse;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final ReviewRepository reviewRepository;
    private final RestTemplate restTemplate;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Value("${kakao.api.base-url:https://dapi.kakao.com}")
    private String kakaoApiBaseUrl;

    public RestaurantService(RestaurantRepository restaurantRepository, ReviewRepository reviewRepository) {
        this.restaurantRepository = restaurantRepository;
        this.reviewRepository = reviewRepository;
        this.restTemplate = new RestTemplate();
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants (bounds - 리뷰 있는 식당만)
    // ---------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getReviewedRestaurantsInBounds(
            Double swLng, Double swLat, Double neLng, Double neLat) {

        List<Restaurant> restaurants = restaurantRepository.findReviewedInBounds(swLng, swLat, neLng, neLat);
        if (restaurants.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = restaurants.stream().map(Restaurant::getId).toList();
        Map<Long, ReviewRepository.RestaurantReviewStats> statsMap = reviewRepository
                .findStatsByRestaurantIds(ids)
                .stream()
                .collect(Collectors.toMap(ReviewRepository.RestaurantReviewStats::getRestaurantId, s -> s));

        return restaurants.stream()
                .map(r -> toRestaurantResponse(r, statsMap.get(r.getId())))
                .toList();
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants/{id}
    // ---------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        List<ReviewRepository.RestaurantReviewStats> statsList =
                reviewRepository.findStatsByRestaurantIds(List.of(id));

        ReviewRepository.RestaurantReviewStats stats = statsList.isEmpty() ? null : statsList.get(0);
        return toRestaurantResponse(restaurant, stats);
    }

    // ---------------------------------------------------------------------------
    // GET /restaurants/search (Kakao 단일 키워드 프록시)
    // ---------------------------------------------------------------------------
    public List<RestaurantSearchResponse> searchByKeyword(String query, Double x, Double y, int page) {
        URI uri = UriComponentsBuilder
                .fromUriString(kakaoApiBaseUrl)
                .path("/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("sort", "distance")
                .queryParam("page", page)
                .queryParam("size", 15)
                .queryParamIfPresent("x", Optional.ofNullable(x))
                .queryParamIfPresent("y", Optional.ofNullable(y))
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoApiResponseDto> response = restTemplate.exchange(
                    uri, HttpMethod.GET, httpEntity, KakaoApiResponseDto.class);

            KakaoApiResponseDto body = response.getBody();
            if (body == null || body.getDocuments() == null) {
                return Collections.emptyList();
            }

            return body.getDocuments().stream()
                    .map(doc -> new RestaurantSearchResponse(
                            doc.getId(),
                            doc.getPlaceName(),
                            extractMainCategory(doc.getCategoryName()),
                            doc.getRoadAddressName().isEmpty() ? doc.getAddressName() : doc.getRoadAddressName(),
                            Double.parseDouble(doc.getX()),
                            Double.parseDouble(doc.getY()),
                            doc.getPlaceUrl(),
                            doc.getDistance() != null && !doc.getDistance().isEmpty()
                                    ? Double.parseDouble(doc.getDistance()) : null
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Kakao 검색 API 호출 중 오류 발생 (query: {})", query, e);
            throw new BusinessException(ErrorCode.KAKAO_API_ERROR);
        }
    }

    // ---------------------------------------------------------------------------
    // GET /restaurant/nearby (기존 엔드포인트 유지 - 단일 키워드 "음식점"으로 단순화)
    // ---------------------------------------------------------------------------
    @Transactional
    public List<Restaurant> saveAndGetRestaurantsAround(Double x, Double y, int radius) {

        Map<String, Restaurant> restaurantMap = new HashMap<>();

        URI uri = UriComponentsBuilder
                .fromUriString(kakaoApiBaseUrl)
                .path("/v2/local/search/keyword.json")
                .queryParam("query", "음식점")
                .queryParam("x", x)
                .queryParam("y", y)
                .queryParam("radius", radius)
                .queryParam("sort", "distance")
                .queryParam("page", 1)
                .queryParam("size", 15)
                .encode()
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> httpEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<KakaoApiResponseDto> response = restTemplate.exchange(
                    uri, HttpMethod.GET, httpEntity, KakaoApiResponseDto.class);

            KakaoApiResponseDto body = response.getBody();
            List<KakaoApiResponseDto.Document> documents = body.getDocuments();

            if (documents != null) {
                for (KakaoApiResponseDto.Document doc : documents) {
                    if (restaurantMap.containsKey(doc.getId())) {
                        continue;
                    }

                    String mainCategory = extractMainCategory(doc.getCategoryName());

                    Restaurant existingStore = restaurantRepository.findByKakaoApiId(doc.getId());
                    if (existingStore != null) {
                        restaurantMap.put(doc.getId(), existingStore);
                        continue;
                    }

                    Restaurant restaurant = Restaurant.builder()
                            .kakaoApiId(doc.getId())
                            .name(doc.getPlaceName())
                            .category(mainCategory)
                            .address(doc.getRoadAddressName().isEmpty() ? doc.getAddressName() : doc.getRoadAddressName())
                            .placeUrl(doc.getPlaceUrl())
                            .x(Double.parseDouble(doc.getX()))
                            .y(Double.parseDouble(doc.getY()))
                            .build();

                    restaurantRepository.save(restaurant);
                    restaurantMap.put(doc.getId(), restaurant);
                }
            }
        } catch (Exception e) {
            log.error("API 호출 중 에러 발생", e);
        }

        List<Restaurant> resultList = new ArrayList<>(restaurantMap.values());
        resultList.sort(Comparator.comparingDouble(r -> getDistance(y, x, r.getY(), r.getX())));
        return resultList;
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------
    private RestaurantResponse toRestaurantResponse(Restaurant r, ReviewRepository.RestaurantReviewStats stats) {
        long reviewCount = stats != null && stats.getCnt() != null ? stats.getCnt() : 0L;
        double avgRating = stats != null && stats.getAvg() != null ? stats.getAvg() : 0.0;
        return new RestaurantResponse(
                r.getId(), r.getKakaoApiId(), r.getName(), r.getCategory(),
                r.getAddress(), r.getX(), r.getY(), r.getPlaceUrl(),
                reviewCount, avgRating);
    }

    private String extractMainCategory(String fullCategory) {
        if (fullCategory != null && fullCategory.contains(">")) {
            String[] split = fullCategory.split(">");
            if (split.length > 1) {
                return split[1].trim();
            }
        }
        return "음식점";
    }

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2);
    }
}
