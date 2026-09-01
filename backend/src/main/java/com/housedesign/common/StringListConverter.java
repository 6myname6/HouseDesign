package com.housedesign.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/**
 * JPA 属性转换器：在 {@code List<String>} 与数据库中的 JSON 字符串之间互转，
 * 用于把帖子图片地址列表以 JSON 文本形式存储于 TEXT 列。
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 实体属性 → 数据库列：列表序列化为 JSON，空列表写为 "[]"。 */
    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 数据库列 → 实体属性：JSON 反序列化为列表，空/非法数据返回空列表。 */
    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
