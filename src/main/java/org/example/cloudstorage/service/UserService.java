package org.example.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.UserDetailsDto;
import org.example.cloudstorage.dto.UserDto;
import org.example.cloudstorage.entity.Role;
import org.example.cloudstorage.entity.User;
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

    public void registration(UserDto userDto) {

        var user = User.builder()
                .username(userDto.username())
                .password(passwordEncoder.encode(userDto.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
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
