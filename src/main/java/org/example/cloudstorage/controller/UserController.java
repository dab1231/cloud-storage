package org.example.cloudstorage.controller;

import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.controller.api.UserControllerApi;
import org.example.cloudstorage.dto.response.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerApi {

  @Override
  public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {

    var username = principal.getName();
    return ResponseEntity.status(HttpStatus.OK).body(new UserResponse(username));
  }
}
