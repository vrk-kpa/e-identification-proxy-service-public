/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.identification.proxy.session;

import fi.vm.kapa.identification.type.AuthMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
@Component
public class UidToUserSessionsCache {
    private static final Logger logger = LoggerFactory.getLogger(UidToUserSessionsCache.class);

    private final ConcurrentMap<String,Map<AuthMethod,Session>> sessionsCache;

    @Autowired
    private SessionStatusPrinter sessionStatusPrinter;

    public UidToUserSessionsCache() {
        sessionsCache = new ConcurrentHashMap<>();
    }

    UidToUserSessionsCache(@NotNull ConcurrentMap<String,Map<AuthMethod,Session>> sessions) {
        sessionsCache = sessions;
    }

    public Session getSessionByKeyAndAuthMethod(String key, AuthMethod authMethod) {
        return sessionsCache.getOrDefault(key, Collections.emptyMap()).get(authMethod);
    }

    /**
     * This is used during the authentication process session finalising phase.
     */
    public Map<AuthMethod,Session> getSessionDTOMapByKey(String key) {
        return sessionsCache.get(key);
    }

    public void insertIntoSessionCache(String key, AuthMethod authMethod, Session session) {
        Map<AuthMethod,Session> authMethodSessionDTOMap = sessionsCache.getOrDefault(key, Collections.emptyMap());
        if (!authMethodSessionDTOMap.isEmpty()) {
            //Existing session with same authMethod is overwritten
            authMethodSessionDTOMap.put(authMethod, session);
        } else {
            ConcurrentMap<AuthMethod,Session> newSessionDTOMap = new ConcurrentHashMap<>();
            newSessionDTOMap.put(authMethod, session);
            authMethodSessionDTOMap = sessionsCache.putIfAbsent(key, newSessionDTOMap);
            //Check and update existing entry
            if (authMethodSessionDTOMap != null) {
                authMethodSessionDTOMap.put(authMethod, session);
            }
        }
    }

    /**
     * Called when uid is set as the final key or session data is updated
     */
    public void replaceSessionCacheKey(String oldKey, String newKey, AuthMethod authMethod, Session session) {
        sessionsCache.remove(oldKey);
        insertIntoSessionCache(newKey, authMethod, session);
    }

    /**
     * Invalidates the session, sets invalidated attribute for all auth methods in session
     */
    public void invalidateCachedSessionsByKey(String key) {
        sessionsCache.getOrDefault(key, Collections.emptyMap()).forEach((authMethod, sessionDTO) -> sessionDTO.setVtjDataInvalid(true));
    }

    /**
     * Checks that no session is invalid
     */
    public boolean invalidSessionsInCacheByKey(String key) {
        boolean invalidSessions = false;
        Map<AuthMethod,Session> authMethodSessionDTOMap = sessionsCache.getOrDefault(key, Collections.emptyMap());
        if (!authMethodSessionDTOMap.isEmpty()) {
            for (Map.Entry<AuthMethod,Session> sessionDTOEntry : authMethodSessionDTOMap.entrySet()) {
                if (sessionDTOEntry.getValue().isVtjDataInvalid()) {
                    invalidSessions = true;
                }
            }
        }
        return invalidSessions;
    }

    public Session removeFromSessionCache(String key, AuthMethod authMethod) {
        Map<AuthMethod,Session> authMethodSessionDTOMap = sessionsCache.getOrDefault(key, Collections.emptyMap());
        Session removedSession = null;
        if (!authMethodSessionDTOMap.isEmpty()) {
            removedSession = authMethodSessionDTOMap.remove(authMethod);
            if (authMethodSessionDTOMap.isEmpty()) {
                sessionsCache.remove(key);
            }
        }
        return removedSession != null ? removedSession : new Session();
    }

    public Map<String,Map<AuthMethod,Session>> getSessionsCache() {
        return sessionsCache;
    }

    public boolean cacheContainsKey(String key) {
        return sessionsCache.containsKey(key);
    }

    public void debugLogSessionStatus() {
        logger.debug("----------Sessions status----------");
        logger.debug("-----------------------------------");
        sessionsCache.forEach((key, sessionDTOMap) -> {
            logger.debug("***KEY: " + key.toString());
            sessionDTOMap.forEach((authMethod, sessionDTO) -> {
                logger.debug("******AUTHMETHOD: " + authMethod.toString());
                logger.debug(sessionStatusPrinter.printSessionStatus(sessionDTO));
            });
        });
    }

}