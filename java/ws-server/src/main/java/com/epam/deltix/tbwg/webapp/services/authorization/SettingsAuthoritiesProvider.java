package com.epam.deltix.tbwg.webapp.services.authorization;

import com.epam.deltix.tbwg.webapp.settings.AuthoritiesSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class SettingsAuthoritiesProvider implements AuthoritiesProvider {

    private final ConcurrentMap<String, List<String>> users = new ConcurrentHashMap<>();

    @Autowired
    public SettingsAuthoritiesProvider(AuthoritiesSettings settings) {
        settings.getUsers().forEach(user -> {
            users.put(user.getUsername(), user.getAuthorities());
        });
    }

    @Override
    public List<GrantedAuthority> getAuthorities(String username) {
        return users.getOrDefault(username, new ArrayList<>()).stream()
            .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}
