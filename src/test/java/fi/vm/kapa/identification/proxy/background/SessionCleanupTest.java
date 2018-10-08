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
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SessionCleanupTest {

    private int activeSessionsTTL = 40;
    private int failedSessionsTTL = 10;

    private SessionCleanup sessionCleanup;

    @Before
    public void setUp() throws Exception {
        UidToUserSessionsCache uidToUserSessionsCache = new UidToUserSessionsCache();
        sessionCleanup = new SessionCleanup(activeSessionsTTL,
                failedSessionsTTL,
                uidToUserSessionsCache);
    }

    @Test
    public void runCleanup() throws Exception {
        UidToUserSessionsCache cacheSpy = spy(UidToUserSessionsCache.class);
        sessionCleanup = new SessionCleanup(activeSessionsTTL,
                failedSessionsTTL,
                cacheSpy);
        HashMap<String, Map<AuthMethod,Session>> sessions = new HashMap<>();
        sessions.put("TEST_KEY_1", getAuthMethodToSession(AuthMethod.fLoA3, true));
        sessions.put("TEST_KEY_2", getAuthMethodToSession(AuthMethod.fLoA2, true));

        DateTimeUtils.setCurrentMillisFixed(activeSessionsTTL * 60000 + System.currentTimeMillis() + 1);
        when(cacheSpy.getSessionsCache()).thenReturn(sessions);

        sessionCleanup.runCleanup();
        verify(cacheSpy, times(1)).getSessionsCache();
        verify(cacheSpy, times(1)).removeFromSessionCache("TEST_KEY_1", AuthMethod.fLoA3);
        verify(cacheSpy, times(1)).removeFromSessionCache("TEST_KEY_2", AuthMethod.fLoA2);
        verifyNoMoreInteractions(cacheSpy);
    }

    @Test
    public void getSessionsToBeRemovedDoesNotReturnNewlyCreatedSessions() throws Exception {
        Map<String, Map<AuthMethod,Session>> sessionsCache = new HashMap<>();

        Map<AuthMethod,Session> authToSession = getAuthMethodToSession(AuthMethod.fLoA3, true);
        sessionsCache.put("TEST_KEY_1", authToSession);

        Map<AuthMethod,Session> authToSession2 = getAuthMethodToSession(AuthMethod.INIT, false);
        sessionsCache.put("TEST_KEY_2", authToSession2);

        DateTimeUtils.setCurrentMillisFixed(failedSessionsTTL * 60000 + System.currentTimeMillis() - 1);

        List<Map.Entry<String, AuthMethod>> sessionsToBeRemoved = sessionCleanup.getSessionsToBeRemoved(sessionsCache);
        assertTrue(sessionsToBeRemoved.isEmpty());
    }

    @Test
    public void getSessionsToBeRemovedReturnsNotValidatedSessionsAfterFailedSessionTTL() throws Exception {
        Map<String, Map<AuthMethod,Session>> sessionsCache = new HashMap<>();

        Map<AuthMethod,Session> authToSession = getAuthMethodToSession(AuthMethod.fLoA3, true);
        sessionsCache.put("TEST_KEY_1", authToSession);
        Map<AuthMethod,Session> expiringFailedSession = getAuthMethodToSession(AuthMethod.INIT, false);
        sessionsCache.put("TEST_KEY_2", expiringFailedSession);

        DateTimeUtils.setCurrentMillisFixed(failedSessionsTTL * 60000 + System.currentTimeMillis() + 1);

        List<Map.Entry<String, AuthMethod>> sessionsToBeRemoved = sessionCleanup.getSessionsToBeRemoved(sessionsCache);
        assertEquals(1, sessionsToBeRemoved.size());
        assertEquals("TEST_KEY_2", sessionsToBeRemoved.get(0).getKey());
    }

    @Test
    public void getSessionsToBeRemovedReturnsExpiredSessionsAfterActiveSessionTTL() throws Exception {
        Map<String, Map<AuthMethod,Session>> sessionsCache = new HashMap<>();

        Map<AuthMethod,Session> authToSession = getAuthMethodToSession(AuthMethod.fLoA3, true);
        sessionsCache.put("TEST_KEY_1", authToSession);
        Map<AuthMethod,Session> authToSession2 = getAuthMethodToSession(AuthMethod.fLoA2, true);
        sessionsCache.put("TEST_KEY_2", authToSession2);

        DateTimeUtils.setCurrentMillisFixed(activeSessionsTTL * 60000 + System.currentTimeMillis() + 1);

        List<Map.Entry<String, AuthMethod>> sessionsToBeRemoved = sessionCleanup.getSessionsToBeRemoved(sessionsCache);
        assertEquals(2, sessionsToBeRemoved.size());
    }

    Map<AuthMethod,Session> getAuthMethodToSession(AuthMethod authMethod, boolean validated) {
        Map<AuthMethod,Session> authToSession = new HashMap<>();
        Session validatedSession = getSessionWithTimestampSetAndValidatedStatus(validated);
        authToSession.put(authMethod, validatedSession);
        return authToSession;
    }

    Session getSessionWithTimestampSetAndValidatedStatus(boolean validated) {
        Session session = new Session();
        session.setValidated(validated);
        session.setTimestamp();
        return session;
    }

}