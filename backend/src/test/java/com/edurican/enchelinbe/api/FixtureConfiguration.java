package com.edurican.enchelinbe.api;

import com.edurican.enchelinbe.api.fixture.AuthFixture;
import com.edurican.enchelinbe.api.fixture.RestaurantFixture;
import com.edurican.enchelinbe.api.fixture.ReviewFixture;
import com.edurican.enchelinbe.api.fixture.ReviewSummaryFixture;
import com.edurican.enchelinbe.auth.JwtProvider;
import com.edurican.enchelinbe.repository.RestaurantRepository;
import com.edurican.enchelinbe.repository.UserRepository;
import com.edurican.enchelinbe.service.ReviewSummaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@TestConfiguration
public class FixtureConfiguration {

  @Bean
  @Scope("prototype")
  ReviewFixture reviewFixture(Environment environment, ObjectMapper objectMapper,
      RestaurantRepository restaurantRepository) {
    return ReviewFixture.create(environment, objectMapper, restaurantRepository);
  }

  @Bean
  @Scope("prototype")
  RestaurantFixture restaurantFixture(RestaurantRepository restaurantRepository) {
    return new RestaurantFixture(restaurantRepository);
  }

  @Bean
  @Scope("prototype")
  AuthFixture authFixture(UserRepository userRepository, JwtProvider jwtProvider) {
    return new AuthFixture(userRepository, jwtProvider);
  }

  @Bean
  @Scope("prototype")
  ReviewSummaryFixture reviewSummaryFixture(
      Environment environment, ObjectMapper objectMapper,
      ReviewSummaryService reviewSummaryService) {
    return ReviewSummaryFixture.create(environment, objectMapper, reviewSummaryService);
  }
}
