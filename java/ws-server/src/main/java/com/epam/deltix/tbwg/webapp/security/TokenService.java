package com.epam.deltix.tbwg.webapp.security;

import org.springframework.security.core.Authentication;

public interface TokenService {

    Authentication          extract(String token);

}
