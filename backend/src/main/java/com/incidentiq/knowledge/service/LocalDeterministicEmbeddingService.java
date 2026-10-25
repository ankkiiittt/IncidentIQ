package com.incidentiq.knowledge.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Service
public class LocalDeterministicEmbeddingService implements EmbeddingService {

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String cleaned = text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s_-]", " ");
        String[] words = cleaned.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isBlank()) continue;

            // Unigram projection
            projectToken(word, 1.0f, vector);

            // Subword 3-char n-grams for typo & stem matching
            if (word.length() > 3) {
                for (int j = 0; j <= word.length() - 3; j++) {
                    String trigram = word.substring(j, j + 3);
                    projectToken(trigram, 0.4f, vector);
                }
            }

            // Bigram projection
            if (i < words.length - 1) {
                String bigram = word + "_" + words[i + 1];
                projectToken(bigram, 1.2f, vector);
            }
        }

        // L2 normalize
        float sumSquares = 0.0f;
        for (float v : vector) {
            sumSquares += v * v;
        }

        if (sumSquares > 0.0f) {
            float norm = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < vector.length; i++) {
                vector[i] = vector[i] / norm;
            }
        }

        return vector;
    }

    private void projectToken(String token, float weight, float[] vector) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));

            // Use bytes of hash to seed 4 coordinates across the 384 dimensions
            for (int k = 0; k < 4; k++) {
                int b1 = hash[k * 4] & 0xFF;
                int b2 = hash[k * 4 + 1] & 0xFF;
                int index = ((b1 << 8) | b2) % DIMENSIONS;

                int signByte = hash[k * 4 + 2];
                float sign = (signByte % 2 == 0) ? 1.0f : -1.0f;

                vector[index] += sign * weight;
            }
        } catch (NoSuchAlgorithmException ignored) {
        }
    }
}
