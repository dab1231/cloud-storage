package org.example.cloudstorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.request.UserReqDto;
import org.example.cloudstorage.dto.response.UserRespDto;
import org.example.cloudstorage.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserRespDto> registration(@RequestBody UserReqDto userReqDto,
                                                    HttpServletRequest request) {

        var user = userService.registration(userReqDto);
        request.getSession(true);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserRespDto(user.getUsername()));
    }
}
