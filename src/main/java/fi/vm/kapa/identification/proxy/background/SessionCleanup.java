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
package fi.vm.kapa.identification.proxy.background;

import fi.vm.kapa.identification.proxy.session.Session;
import fi.vm.kapa.identification.proxy.session.UidToUserSessionsCache;
import fi.vm.kapa.identification.type.AuthMethod;
import javafx.util.Pair;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SessionCleanup {

    private static final Logger logger = LoggerFactory.getLogger(SessionCleanup.class);

    // in minutes
    private int activeSessionsTTL;
    private int failedSessionsTTL;

    private UidToUserSessionsCache uidToUserSessionsCache;

    @SuppressWarnings("unused")
    private SessionCleanup() {
    }

    @Autowired
    SessionCleanup(@Value("${sessions.cache.active.ttl}") int activeSessionsTTL,
                   @Value("${sessions.cache.failed.ttl}") int failedSessionsTTL,
                   UidToUserSessionsCache uidToUserSessionsCache
    ) {
        this.activeSessionsTTL = activeSessionsTTL;
        this.failedSessionsTTL = failedSessionsTTL;
        this.uidToUserSessionsCache = uidToUserSessionsCache;
    }

    public void runCleanup() {
        try {
            Map<String,Map<AuthMethod,Session>> sessionsInCache = uidToUserSessionsCache.getSessionsCache();

            long originalSize = getAuthSessionsSize(sessionsInCache);
            logger.info("Currently there are {} sessions in cache", originalSize);

            List<Pair<String,AuthMethod>> toBeRemoved = getSessionsToBeRemoved(sessionsInCache);
            toBeRemoved.forEach(pair -> uidToUserSessionsCache.removeFromSessionCache(pair.getKey(), pair.getValue()));

            long finalSize = getAuthSessionsSize(sessionsInCache);
            logger.info("Removed {} expired sessions from cache", originalSize - finalSize);
        } catch (Exception e) {
            logger.error("Error running session cleanup", e);
        }
    }

    private long getAuthSessionsSize(Map<String,Map<AuthMethod,Session>> sessionsInCache) {
        long size = 0;
        for (Map<AuthMethod,Session> sessionDTOMap : sessionsInCache.values()) {
            size += sessionDTOMap.size();
        }
        return size;
    }

    long getActiveTTLThreshold() {
        return DateTimeUtils.currentTimeMillis() - activeSessionsTTL * 60000;
    }

    long getFailedTTLThreshold() {
        return DateTimeUtils.currentTimeMillis() - failedSessionsTTL * 60000;
    }

    List<Pair<String,AuthMethod>> getSessionsToBeRemoved(Map<String,Map<AuthMethod,Session>> sessionsInCache) {
        long activeTTLThreshold = getActiveTTLThreshold();
        long failedTTLThreshold = getFailedTTLThreshold();
        List<Pair<String,AuthMethod>> toBeRemoved = new ArrayList<>();
        sessionsInCache.forEach((key, sessionDTOMap) ->
                sessionDTOMap.forEach((authMethod, sessionDTO) -> {
                    if ((sessionDTO.isValidated() && sessionDTO.getTimestamp() < activeTTLThreshold) ||
                            (!sessionDTO.isValidated() && sessionDTO.getTimestamp() < failedTTLThreshold)) {
                        toBeRemoved.add(new Pair<>(key, authMethod));
                    }
                }));
        return toBeRemoved;
    }
}
