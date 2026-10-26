package com.example.learning.opensearch.mappings.dto;

import java.util.Map;

public record IndexMappingResponse(String indexName, Map<String, Object> mapping) {
}
