package com.actionth.membership.converter;

import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.util.List;

public class SelectionAnswerConverter implements AttributeConverter<List<SelectionAnswerDto>, String> {

  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<SelectionAnswerDto> attribute) {
    try {
      return mapper.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not convert selectionAnswers to JSON", e);
    }
  }


  @Override
  public List<SelectionAnswerDto> convertToEntityAttribute(String dbData) {
    try {
      if (dbData == null || dbData.trim().isEmpty()) {
        return null;
      }
      return mapper.readValue(dbData, new TypeReference<>() {
      });
    } catch (Exception e) {
      return List.of();
    }
  }

}
