package org.example.cloudstorage.security;

import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new UserDetailsDto(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User with username " + username + " not found"));
    }
}
