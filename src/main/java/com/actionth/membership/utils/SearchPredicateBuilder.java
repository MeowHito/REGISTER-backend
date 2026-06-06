package com.actionth.membership.utils;

import javax.persistence.criteria.*;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.exception.InvalidSearchFormatException;
import lombok.experimental.UtilityClass;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@UtilityClass
public class SearchPredicateBuilder {

  public static <T> List<Predicate> build(
      CriteriaBuilder cb,
      Root<T> root,
      PagingData paging,
      Function<PagingData.Search, Predicate> overrideHandler) {

    List<Predicate> predicates = new ArrayList<>();

    if (paging == null)
      return predicates;

    if (paging.getSearchField() != null && paging.getSearchText() != null) {
      predicates.add(cb.like(
          cb.lower(root.get(paging.getSearchField())),
          "%" + paging.getSearchText().toLowerCase() + "%"));
    }

    if (paging.getSearch() != null) {
      for (PagingData.Search s : paging.getSearch()) {
        String field = s.getSearchField();
        String text = s.getSearchText();
        String type = s.getSearchType();

        if (field != null && text != null) {
          try {
            Predicate predicate = null;
            if (overrideHandler != null) {
              predicate = overrideHandler.apply(s);
            }

            if (predicate == null) {
              String searchType = (type != null) ? type.toUpperCase() : "LIKE";
              predicate = switch (searchType) {
                case "DATERANGE" -> {
                  Path<OffsetDateTime> dateRangePath = root.get(field);
                  String[] parts = text.split(",");
                  if (parts.length == 2) {
                    OffsetDateTime start = OffsetDateTime.parse(parts[0]);
                    OffsetDateTime end = OffsetDateTime.parse(parts[1]);
                    yield cb.between(dateRangePath, start, end);
                  } else {
                    throw new InvalidSearchFormatException(field, text, "start_date,end_date");
                  }
                }
                case "DATE" -> {
                  Path<OffsetDateTime> datePath = root.get(field);
                  yield cb.equal(datePath, OffsetDateTime.parse(text));
                }
                case "BOOLEAN" -> cb.equal(root.get(field), Boolean.parseBoolean(text));
                case "INTEGER" -> cb.equal(root.get(field), Integer.parseInt(text));
                case "LONG" -> cb.equal(root.get(field), Long.parseLong(text));
                case "EQUAL" -> cb.equal(root.get(field), text);
                default -> cb.like(cb.lower(root.get(field)), "%" + text.toLowerCase() + "%");
              };
            }
            predicates.add(predicate);

          } catch (DateTimeParseException | NumberFormatException e) {
            throw new InvalidSearchFormatException("Invalid value for field: " + field + ", value: " + text, e);
          }
        }
      }
    }

    return predicates;
  }
}
