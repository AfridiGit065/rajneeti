package com.rajneeti.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rajneeti.entity.PendingAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JPA converter that serializes a {@link PendingAction} to and from a JSON column
 * on the {@code matches} table.
 */
@Slf4j
@Component
@Converter(autoApply = true)
@RequiredArgsConstructor
public class PendingActionConverter implements AttributeConverter<PendingAction, String> {

    private final ObjectMapper objectMapper;

    @Override
    public String convertToDatabaseColumn(PendingAction attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize pending action to JSON", ex);
            throw new IllegalArgumentException("Unable to serialize pending action", ex);
        }
    }

    @Override
    public PendingAction convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, PendingAction.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to deserialize pending action from JSON", ex);
            throw new IllegalArgumentException("Unable to deserialize pending action", ex);
        }
    }
}