package com.incidentiq.knowledge.entity;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PGvectorConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null || attribute.length == 0) {
            return null;
        }
        return new PGvector(attribute).getValue();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return new PGvector(dbData).toArray();
        } catch (Exception e) {
            String clean = dbData.replace("[", "").replace("]", "").trim();
            if (clean.isEmpty()) {
                return new float[0];
            }
            String[] parts = clean.split(",");
            float[] res = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                res[i] = Float.parseFloat(parts[i].trim());
            }
            return res;
        }
    }
}
