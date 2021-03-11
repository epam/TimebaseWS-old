package com.epam.deltix.tbwg.webapp.services.oid;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.tbwg.webapp.settings.UserInfoSettings;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Lazy
public class OpenIdAuthProvider {
    private static final Log LOGGER = LogFactory.getLog(OpenIdAuthProvider.class);

    private static final List<String> NAME_KEY_DEFAULT = Collections.singletonList("name");

    private final ParameterizedTypeReference<Map<String, Object>> mapTypeReference = new ParameterizedTypeReference<Map<String,Object>>() {
    };

    private final RestTemplate rest = new RestTemplate();

    private final AuthInfoService authInfoService;
    private final UserInfoSettings userInfoSettings;

    public OpenIdAuthProvider(AuthInfoService authInfoService,
                              UserInfoSettings userInfoSettings)
    {
        this.authInfoService = authInfoService;
        this.userInfoSettings = userInfoSettings;
    }

    public String getUsername(CharSequence token) {
        String userInfoUrl = authInfoService.getUserInfoUrl();
        if (userInfoUrl == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>("", headers);

        final ResponseEntity<Map<String, Object>> response = rest.exchange(userInfoUrl, HttpMethod.GET, entity, mapTypeReference);
        if (response.getStatusCode() != HttpStatus.OK)
            throw new RuntimeException("Request " + userInfoUrl + " failed: " + response.getStatusCode());

        final Map<String, Object> body = response.getBody();
        if (body == null)
            throw new RuntimeException("Cannot acquire username.");

        final String result = getFromMap(body, userInfoSettings.getUsernameKey());
        if (result != null)
            return result;

        return getFromMap(body, NAME_KEY_DEFAULT);
    }

    private static String getFromMap(Map<String, Object> map, List<String> key) {
        if (key == null) {
            return null;
        }

        Object value = null;
        for (int i = 0; i < key.size(); ++i) {
            value = map.get(key.get(i));
            if (i < key.size() - 1) {
                if (value instanceof Map) {
                    //noinspection unchecked
                    map = (Map<String, Object>) value;
                } else {
                    return null;
                }
            }
        }

        if (value instanceof String)
            return (String) value;

        return null;
    }

}
