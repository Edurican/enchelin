package com.edurican.enchelinbe.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SourceHashCalculator {

    public String compute(List<Review> reviews) {
        String input = reviews.stream()
                .sorted(Comparator.comparingLong(r -> r.getId()))
                .map(r -> r.getId() + ":" + r.getUpdatedAt().toEpochSecond(java.time.ZoneOffset.UTC))
                .collect(Collectors.joining("|"));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
