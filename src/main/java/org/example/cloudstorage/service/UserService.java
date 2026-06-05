package org.example.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.UserDetailsDto;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.entity.Role;
import org.example.cloudstorage.entity.User;
import org.example.cloudstorage.exception.UserAlreadyExistsException;
import org.example.cloudstorage.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse registration(UserRequest userRequest) {

        userRepository.findByUsername(userRequest.username())
                .ifPresent(user ->
                {throw new UserAlreadyExistsException("User with username " + userRequest.username() + " already exists");}
                );

        var user = User.builder()
                .username(userRequest.username())
                .password(passwordEncoder.encode(userRequest.password()))
                .role(Role.USER)
                .build();

        var savedUser = userRepository.save(user);

        return UserResponse.builder()
                .username(savedUser.getUsername())
                .build();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new UserDetailsDto(
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " not found"));
    }
}
