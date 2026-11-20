package com.sistemacontable.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** JPA 3.1/EclipseLink 4: mapea instantes a timestamptz mediante el tipo JDBC 4.2 con zona. */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, OffsetDateTime> {
    @Override public OffsetDateTime convertToDatabaseColumn(Instant value) { return value == null ? null : value.atOffset(ZoneOffset.UTC); }
    @Override public Instant convertToEntityAttribute(OffsetDateTime value) { return value == null ? null : value.toInstant(); }
}
