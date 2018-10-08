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
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UidToUserSessionsCacheTest {

    @Test
    public void getSessionByKeyAndAuthMethod() {
        ConcurrentMap<String, Map<AuthMethod,Session>> tokenSessions = new ConcurrentHashMap<>();
        tokenSessions.put("TEST_KEY", getUserSessionsWithOneSession(AuthMethod.fLoA3));
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache(tokenSessions);
        Session session = uidToUserSessionsCache.getSessionByKeyAndAuthMethod("TEST_KEY", AuthMethod.fLoA3);
        assertNotNull(session);
    }

    @Test
    public void getSessionByKeyAndAuthMethodReturnNullIfUserSessionsNotFound() {
        ConcurrentMap<String, Map<AuthMethod,Session>> uidToUserSessions = new ConcurrentHashMap<>();
        uidToUserSessions.put("TEST_KEY", getUserSessionsWithOneSession(AuthMethod.fLoA3));
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache(uidToUserSessions);
        assertEquals(null, uidToUserSessionsCache.getSessionByKeyAndAuthMethod("TOKEN_NOT_FOUND", AuthMethod.fLoA3));
    }

    @Test
    public void getSessionByKeyAndAuthMethodReturnNullIfUserSessionWithAuthMethodNotFound() {
        ConcurrentMap<String, Map<AuthMethod,Session>> uidToUserSessions = new ConcurrentHashMap<>();
        uidToUserSessions.put("TEST_KEY", getUserSessionsWithOneSession(AuthMethod.fLoA3));
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache(uidToUserSessions);
        assertEquals(null, uidToUserSessionsCache.getSessionByKeyAndAuthMethod("TEST_KEY", AuthMethod.fLoA2));
    }

    @Test
    public void getSessionDTOMapByKey() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        Map<AuthMethod,Session> sessionDTOMap = uidToUserSessionsCache.getSessionDTOMapByKey("TEST_KEY");
        assertTrue(sessionDTOMap.containsKey(AuthMethod.fLoA3));
    }

    @Test
    public void insertIntoSessionCacheWhenCacheEmpty() {
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache();
        uidToUserSessionsCache.insertIntoSessionCache("TEST_KEY", AuthMethod.fLoA2, new Session());
        Map<String, Map<AuthMethod,Session>> sessionsCache = uidToUserSessionsCache.getSessionsCache();
        assertNotNull(sessionsCache.get("TEST_KEY"));
        assertEquals(1, sessionsCache.get("TEST_KEY").size());
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA2));
    }

    @Test
    public void insertIntoSessionCacheWhenCacheEmptyFakeConcurrentAdd() {
        ConcurrentHashMap sessions = mock(ConcurrentHashMap.class);
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache(sessions);
        // the addition had not yet happened, return empty map
        when(sessions.getOrDefault(any(), anyMap())).thenReturn(new HashMap<>());
        HashMap<Object, Object> addedSessions = new HashMap<>();
        addedSessions.put(AuthMethod.fLoA3, new Session());
        // the addition happened
        when(sessions.putIfAbsent(any(), anyMap())).thenReturn(addedSessions);
        // actual test
        uidToUserSessionsCache.insertIntoSessionCache("TEST_KEY", AuthMethod.fLoA2, new Session());
        Map<String, Map<AuthMethod,Session>> sessionsCache = uidToUserSessionsCache.getSessionsCache();
        assertEquals(2, addedSessions.size());
        assertTrue(addedSessions.containsKey(AuthMethod.fLoA3));
        assertTrue(addedSessions.containsKey(AuthMethod.fLoA2));
    }

    @Test
    public void insertIntoSessionCacheWhenCacheHasUserSessionWithDifferentAuthMethod() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        uidToUserSessionsCache.insertIntoSessionCache("TEST_KEY", AuthMethod.fLoA2, new Session());
        Map<String, Map<AuthMethod,Session>> sessionsCache = uidToUserSessionsCache.getSessionsCache();
        assertNotNull(sessionsCache.get("TEST_KEY"));
        assertEquals(2, sessionsCache.get("TEST_KEY").size());
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA2));
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA3));
    }

    @Test
    public void insertIntoSessionCacheReplacesSessionWhenCacheHasUserSessionWithSameAuthMethod() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        Session session = new Session();
        uidToUserSessionsCache.insertIntoSessionCache("TEST_KEY", AuthMethod.fLoA3, session);
        Map<String, Map<AuthMethod,Session>> sessionsCache = uidToUserSessionsCache.getSessionsCache();
        assertNotNull(sessionsCache.get("TEST_KEY"));
        assertEquals(1, sessionsCache.get("TEST_KEY").size());
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA3));
    }

    @Test
    public void insertIntoSessionCacheAddsSessionWhenCacheHasUserSessionWithDifferentAuthMethod() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        Session session = new Session();
        uidToUserSessionsCache.insertIntoSessionCache("TEST_KEY", AuthMethod.fLoA2, session);
        Map<String, Map<AuthMethod,Session>> sessionsCache = uidToUserSessionsCache.getSessionsCache();
        assertNotNull(sessionsCache.get("TEST_KEY"));
        assertEquals(2, sessionsCache.get("TEST_KEY").size());
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA3));
        assertTrue(sessionsCache.get("TEST_KEY").containsKey(AuthMethod.fLoA2));
    }

    @Test
    public void replaceSessionCacheKey() throws Exception {
        Session oldSession = new Session();
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, oldSession);
        Session newSession = new Session();
        uidToUserSessionsCache.replaceSessionCacheKey("TEST_KEY", "NEW_KEY", AuthMethod.fLoA3, newSession);
        assertFalse(uidToUserSessionsCache.getSessionsCache().containsKey("TEST_KEY"));
        assertTrue(uidToUserSessionsCache.getSessionsCache().containsKey("NEW_KEY"));
        assertEquals(newSession, uidToUserSessionsCache.getSessionsCache().get("NEW_KEY").get(AuthMethod.fLoA3));
    }

    @Test
    public void invalidateCachedSessionsByKey() throws Exception {
        UidToUserSessionsCache sessionsCache = getUidToUserSessionsCacheWithOneTokenSessionTwoSessions("TEST_KEY", AuthMethod.fLoA3, AuthMethod.fLoA2);
        sessionsCache.invalidateCachedSessionsByKey("TEST_KEY");
        Collection<Session> values = sessionsCache.getSessionsCache().get("TEST_KEY").values();
        assertEquals(2, values.size());
        assertTrue(values.stream().allMatch(Session::isVtjDataInvalid));
    }

    @Test
    public void invalidSessionsInCacheByKeyReturnsFalseWhenSessionWithValidVtjDataFound() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        assertFalse(uidToUserSessionsCache.invalidSessionsInCacheByKey("TEST_KEY"));
    }

    @Test
    public void invalidSessionsInCacheByKeyReturnsTrueWhenSessionWithInvalidVtjDataFound() {
        Session session = new Session();
        session.setVtjDataInvalid(true);
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, session);
        assertTrue(uidToUserSessionsCache.invalidSessionsInCacheByKey("TEST_KEY"));
    }

    @Test
    public void removeFromSessionCacheRemovesSessionWhenMoreThanOneSessionFound() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSessionTwoSessions("TEST_KEY", AuthMethod.fLoA3, AuthMethod.fLoA2);
        uidToUserSessionsCache.removeFromSessionCache("TEST_KEY", AuthMethod.fLoA3);
        assertTrue(uidToUserSessionsCache.getSessionsCache().get("TEST_KEY").containsKey(AuthMethod.fLoA2));
        assertFalse(uidToUserSessionsCache.getSessionsCache().get("TEST_KEY").containsKey(AuthMethod.fLoA3));
    }

    @Test
    public void removeFromSessionCacheRemovesSessionWhenOneSessionFoundRemovesTheTokenSession() {
        UidToUserSessionsCache uidToUserSessionsCache = getUidToUserSessionsCacheWithOneTokenSession("TEST_KEY", AuthMethod.fLoA3, new Session());
        uidToUserSessionsCache.removeFromSessionCache("TEST_KEY", AuthMethod.fLoA3);
        assertNull(uidToUserSessionsCache.getSessionsCache().get("TEST_KEY"));
    }

    private UidToUserSessionsCache getUidToUserSessionsCacheWithOneTokenSessionTwoSessions(String token, AuthMethod authMethod, AuthMethod authMethod2) {
        ConcurrentMap<String, Map<AuthMethod,Session>> uidToUserSessions = new ConcurrentHashMap<>();
        HashMap<AuthMethod,Session> userSessions = new HashMap<>();
        userSessions.put(authMethod, new Session());
        userSessions.put(authMethod2, new Session());
        uidToUserSessions.put(token, userSessions);
        return new UidToUserSessionsCache(uidToUserSessions);
    }

    private UidToUserSessionsCache getUidToUserSessionsCacheWithOneTokenSession(String token, AuthMethod authMethod, Session session) {
        ConcurrentMap<String, Map<AuthMethod,Session>> uidToUserSessions = new ConcurrentHashMap<>();
        Map<AuthMethod,Session> userSessions = new HashMap<>();
        userSessions.put(authMethod, session);
        uidToUserSessions.put(token, userSessions);
        return new UidToUserSessionsCache(uidToUserSessions);
    }


    private Map<AuthMethod,Session> getUserSessionsWithOneSession(AuthMethod authMethod) {
        Map<AuthMethod,Session> userSessions = new HashMap<>();
        Session session = new Session();
        userSessions.put(authMethod, session);
        return userSessions;
    }
}