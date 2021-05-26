/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
