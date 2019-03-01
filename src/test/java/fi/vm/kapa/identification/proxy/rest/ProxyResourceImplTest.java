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
package fi.vm.kapa.identification.proxy.rest;

import fi.vm.kapa.identification.dto.ProxyMessageDTO;
import fi.vm.kapa.identification.dto.SessionAttributeDTO;
import fi.vm.kapa.identification.proxy.service.SessionHandlingService;
import fi.vm.kapa.identification.type.ErrorType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.WebApplicationException;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyResourceImplTest {

    @Mock
    SessionHandlingService sessionHandlingService;

    @Autowired
    @InjectMocks
    private ProxyResourceImpl proxyApiController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void fromIdPInitSessionThrows503WhenVtjFailed() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.VTJ_FAILED);
        when(sessionHandlingService.initNewSession(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromIdPInitSession("relyingParty", "", "","0", "key", "TEST_AUTH_METHOD", "0");
        } catch (WebApplicationException e) {
            assertEquals(503, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).initNewSession("relyingParty", "", "","0", "key", "TEST_AUTH_METHOD", "0");
    }

    @Test
    public void fromIdPInitSessionThrows400WhenPhaseIdFailed() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.PHASE_ID_FAILED);
        when(sessionHandlingService.initNewSession(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromIdPInitSession("relyingParty", "", "","0", "key", "TEST_AUTH_METHOD", "0");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).initNewSession("relyingParty","","","0", "key", "TEST_AUTH_METHOD", "0");
    }

    @Test
    public void fromIdPInitSessionThrows500WhenInternalError() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.INTERNAL_ERROR);
        when(sessionHandlingService.initNewSession(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromIdPInitSession("relyingParty", "", "", "0", "key", "TEST_AUTH_METHOD", "0");
        } catch (WebApplicationException e) {
            assertEquals(500, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).initNewSession("relyingParty", "", "","0", "key", "TEST_AUTH_METHOD", "0");
    }

    @Test
    public void fromIdPRequestSessionThrows400WhenPhaseIdFailed() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.PHASE_ID_FAILED);
        when(sessionHandlingService.getSessionById(anyString(), anyString(), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromIdPRequestSession("tokenId", "phaseId", "logtag");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).getSessionById("tokenId", "phaseId", "logtag");
    }

    @Test
    public void fromIdPPurgeSessionThrows400WhenPhaseIdFailed() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.PHASE_ID_FAILED);
        when(sessionHandlingService.removeSessionById(anyString(), anyString(), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromIdPPurgeSession("tokenId", "phaseId", "logtag");
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).removeSessionById("tokenId", "phaseId", "logtag");
    }

    @Test
    public void getSessionAttributesThrows404WhenSessionDataIsEmpty() throws Exception {
        when(sessionHandlingService.getSessionAttributes(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn(new SessionAttributeDTO());
        try {
            proxyApiController.getSessionAttributes("uid", "authMethodOid", "relyingParty", false, "id");
        } catch (WebApplicationException e) {
            assertEquals(404, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).getSessionAttributes("uid", "authMethodOid", "relyingParty", false, "id");
    }

    @Test
    public void fromSPBuildSessionThrows400WhenPhaseIdFailed() throws Exception {
        ProxyMessageDTO proxyMessage = new ProxyMessageDTO();
        proxyMessage.setErrorType(ErrorType.PHASE_ID_FAILED);
        when(sessionHandlingService.buildNewSession(anyString(), anyString(), anyMapOf(String.class, String.class), anyString())).thenReturn(proxyMessage);
        try {
            proxyApiController.fromSPBuildSessionPost("tokenId", "phaseId", "logtag", Collections.emptyMap());
        } catch (WebApplicationException e) {
            assertEquals(400, e.getResponse().getStatus());
        }
        verify(sessionHandlingService, times(1)).buildNewSession("tokenId", "phaseId", Collections.emptyMap(), "logtag");
    }

}