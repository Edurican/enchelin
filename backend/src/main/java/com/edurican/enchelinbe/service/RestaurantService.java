package com.edurican.enchelinbe.service;

import com.edurican.enchelinbe.dto.KakaoApiResponseDto;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantService {

    private final RestaurantRepository  restaurantRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Transactional
    public List<Restaurant> saveAndGetRestaurantsAround(Double x, Double y, int radius) {

        Map<String, Restaurant> restaurantMap = new HashMap<>();

        List<String> searchKeywords = Arrays.asList(
                "한식", "양식", "일식", "중식", "분식", "태국음식", "베트남음식",
                "치킨", "피자", "패스트푸드", "카페", "고기", "술집", "베이커리"
        );

        for (String keyword : searchKeywords) {

            int page = 1;
            while (page <= 3) {

                URI uri = UriComponentsBuilder
                        .fromUriString("https://dapi.kakao.com")
                        .path("/v2/local/search/keyword.json")
                        .queryParam("query", keyword) //
                        .queryParam("x", x)
                        .queryParam("y", y)
                        .queryParam("radius", radius)
                        .queryParam("sort", "distance")
                        .queryParam("page", page)
                        .queryParam("size", 15)
                        .encode()
                        .build()
                        .toUri();

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "KakaoAK " + kakaoApiKey);
                HttpEntity<String> httpEntity = new HttpEntity<>(headers);

                try {
                    ResponseEntity<KakaoApiResponseDto> response = restTemplate.exchange(
                            uri, HttpMethod.GET, httpEntity, KakaoApiResponseDto.class
                    );

                    KakaoApiResponseDto body = response.getBody();
                    List<KakaoApiResponseDto.Document> documents = body.getDocuments();

                    if (documents == null || documents.isEmpty()) {
                        break;
                    }

                    for (KakaoApiResponseDto.Document doc : documents) {

                        if (restaurantMap.containsKey(doc.getId())) {
                            continue;
                        }

                        String fullCategory = doc.getCategoryName();
                        String mainCategory = "음식점";

                        if (fullCategory != null && fullCategory.contains(">")) {
                            String[] split = fullCategory.split(">");
                            if (split.length > 1) {
                                mainCategory = split[1].trim();
                            }
                        }

                        Restaurant existingStore = restaurantRepository.findByKakaoApiId(doc.getId());

                        if (existingStore != null) {
                            if ("음식점".equals(existingStore.getCategory()) && !mainCategory.equals("음식점")) {
                                existingStore.updateCategory(mainCategory);
                            }

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

                    if (body.getMeta().isEnd()) {
                        break;
                    }
                    page++;

                } catch (Exception e) {
                    log.error("API 호출 중 에러 발생 (키워드: {})", keyword, e);
                    break;
                }
            }
            log.info("키워드 [{}] 수집 완료. 현재 누적 개수: {}", keyword, restaurantMap.size());
        }

        List<Restaurant> resultList = new ArrayList<>(restaurantMap.values());

        resultList.sort(Comparator.comparingDouble(r -> getDistance(y, x, r.getY(), r.getX())));

        log.info("최종 총 수집 개수: {}", resultList.size());
        return resultList;
    }

    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2);
    }
}
