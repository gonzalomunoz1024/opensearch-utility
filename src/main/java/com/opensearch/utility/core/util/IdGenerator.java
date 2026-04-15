package com.opensearch.utility.core.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class IdGenerator {

    private static final String[] ID_FIELD_NAMES = {"id", "_id", "documentId", "docId"};

    public String generate() {
        return UUID.randomUUID().toString();
    }

    public String extractOrGenerate(Map<String, Object> data) {
        if (data == null) {
            return generate();
        }

        for (String fieldName : ID_FIELD_NAMES) {
            Object id = data.get(fieldName);
            if (id != null) {
                return id.toString();
            }
        }

        return generate();
    }
}
