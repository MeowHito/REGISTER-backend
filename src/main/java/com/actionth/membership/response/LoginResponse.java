package com.actionth.membership.response;

import com.actionth.membership.model.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LoginResponse {

  @JsonProperty("token")
  private String token;

  @JsonProperty("userInfo")
  private UserDto user;
}
