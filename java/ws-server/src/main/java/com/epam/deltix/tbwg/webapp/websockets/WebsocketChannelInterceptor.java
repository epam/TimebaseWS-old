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
package com.epam.deltix.tbwg.webapp.websockets;

import com.epam.deltix.tbwg.webapp.security.TokenService;
import com.epam.deltix.tbwg.webapp.services.WebSocketSubscriptionService;
import com.epam.deltix.tbwg.webapp.utils.WebSocketUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@SuppressWarnings("deprecation")
@Component
public class WebsocketChannelInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;
    private final WebSocketSubscriptionService webSocketSubscriptionService;

    @Autowired
    public WebsocketChannelInterceptor(final TokenService tokenService,
                                       final WebSocketSubscriptionService webSocketSubscriptionService)
    {
        this.tokenService = tokenService;
        this.webSocketSubscriptionService = webSocketSubscriptionService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        try {
            preSend(message, accessor);
        } catch (Throwable ex) {
            if (accessor.getCommand() == StompCommand.DISCONNECT) {
                try {
                    webSocketSubscriptionService.unsubscribe(accessor.getSessionId());
                } catch (Throwable ignore) {
                }
                return message; // Always disconnect
            }
            throw ex;
        }
        return message;
    }

    private Message<?> preSend(Message<?> message, StompHeaderAccessor accessor) {
        final SimpMessageType messageType = accessor.getMessageType();
        final Authentication authentication = resolveAuthentication(accessor, messageType != SimpMessageType.HEARTBEAT);

        if (messageType == SimpMessageType.HEARTBEAT)
            return message;

        final String sessionId = accessor.getSessionId();

        switch (accessor.getCommand()) {
            case CONNECT:
                if (authentication == null)
                    throw new AccessDeniedException("User not authenticated.");
                break;

            case UNSUBSCRIBE:
                webSocketSubscriptionService.unsubscribe(sessionId, accessor.getSubscriptionId());
                break;

            case DISCONNECT:
                webSocketSubscriptionService.unsubscribe(sessionId);
        }
        return message;
    }

    private Authentication resolveAuthentication(final StompHeaderAccessor accessor, boolean updateHeader) {
        final Authentication authentication = (Authentication) accessor.getHeader("simpUser");
        if (authentication == null) {
            final String token = accessor.getFirstNativeHeader(WebSocketUtils.HEADER_TOKEN);
            if (token != null) {
                final Authentication auth = tokenService.extract(token);
                if (auth == null) {
                    return null;
                }
                if (updateHeader) {
                    accessor.setHeader("simpUser", auth);
                }
                return auth;
            } else {
                return null;
            }
        } else {
            return authentication;
        }
    }

}
