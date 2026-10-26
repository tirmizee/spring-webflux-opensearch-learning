package com.example.learning.opensearch.documentcrud.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.time.Instant;

@Document(indexName = "#{@environment.getProperty('learning.index-name')}", createIndex = false,
        dynamic = Dynamic.STRICT, storeIdInSource = false)
public record ProductDocument(
    @Id String id,
    @MultiField(mainField = @Field(type = FieldType.Text), otherFields = @InnerField(suffix = "raw", type = FieldType.Keyword, ignoreAbove = 256)) String name,
    @Field(type = FieldType.Keyword) String category,
    @Field(type = FieldType.Long) Long priceSatang,
    @Field(type = FieldType.Integer) Integer stock,
    @Field(type = FieldType.Boolean) Boolean available,
    @Field(type = FieldType.Date, format = DateFormat.date_optional_time) Instant createdAt
) {
}
