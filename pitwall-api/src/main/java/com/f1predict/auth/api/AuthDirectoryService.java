package com.f1predict.auth.api;

import com.f1predict.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class AuthDirectoryService implements AuthDirectory {

    private final UserRepository userRepository;

    public AuthDirectoryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<UserSummary> getUsers(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return userRepository.findAllById(userIds).stream()
            .map(u -> new UserSummary(u.getId(), u.getUsername()))
            .toList();
    }
}
