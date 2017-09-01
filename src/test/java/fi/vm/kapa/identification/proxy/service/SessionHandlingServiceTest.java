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
package fi.vm.kapa.identification.proxy.service;

import fi.vm.kapa.identification.dto.ProxyMessageDTO;
import fi.vm.kapa.identification.dto.SessionAttributeDTO;
import fi.vm.kapa.identification.proxy.person.IdentifiedPersonBuilder;
import fi.vm.kapa.identification.proxy.person.GenericPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.*;
import fi.vm.kapa.identification.proxy.exception.InvalidVtjDataException;
import fi.vm.kapa.identification.proxy.exception.RelyingPartyNotFoundException;
import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
import fi.vm.kapa.identification.proxy.utils.SessionHandlingUtils;
import fi.vm.kapa.identification.service.PhaseIdService;
import fi.vm.kapa.identification.type.*;

import fi.vm.kapa.identification.vtj.model.Person;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ContextConfiguration(locations = "classpath:testContext.xml")
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class SessionHandlingServiceTest {

    @Value("${phase.id.shared.secret}")
    private String secretKey;

    @Value("${phase.id.time.interval}")
    private int timeInterval;

    @Value("${phase.id.time.built.interval}")
    private int timeIntervalBuilt;

    @Value("${phase.id.algorithm}")
    private String hmacAlgorithm;

    @Value("${phase.id.step.one}")
    private String stepSessionInit;

    @Value("${phase.id.step.two}")
    private String stepSessionBuild;

    @Value("${phase.id.step.three}")
    private String stepRedirectFromSP;

    @Value("${phase.id.step.four}")
    private String stepGetSession;

    @Value("${phase.id.step.five}")
    private String stepCancel;

    @Mock
    private MetadataService metadataServiceMock;

    @Mock
    private VtjPersonService vtjPersonServiceMock;

    @Mock
    private IdentifiedPersonBuilder identifiedPersonBuilder;

    @Spy
    private UidToUserSessionsCache uidToUserSessionsCache;

    @Autowired
    private SessionAttributeCollector sessionAttributeCollector;

    @Autowired
    @InjectMocks
    private SessionHandlingService sessionHandlingService;

    private PhaseIdService phaseIdInitSession;
    private PhaseIdService phaseIdBuiltSession;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        phaseIdInitSession = new PhaseIdService(secretKey, timeInterval, hmacAlgorithm);
        phaseIdBuiltSession = new PhaseIdService(secretKey, timeIntervalBuilt, hmacAlgorithm);
    }

    @Test
    public void initializeNewSession() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider("service-provider", "", "TUPAS;HST", SessionProfile.VETUMA_SAML2, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "0", "testkey", "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);
        Assert.assertNotNull(message.getTokenId());
        Assert.assertNotNull(message.getPhaseId());
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertTrue(phaseIdInitSession.verifyPhaseId(message.getPhaseId(), message.getTokenId(), stepSessionInit));

    }

    @Test
    public void initExistingSession() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider("service-provider", "", "TUPAS;HST", SessionProfile.VETUMA_SAML2, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);
        Assert.assertNotNull(message.getTokenId());
        Assert.assertNotNull(message.getPhaseId());
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
    }

    @Test
    public void initNewSessionFailsWhenRelyingPartyAuthenticationMethodIsBlank() throws RelyingPartyNotFoundException {
        ServiceProvider relyingParty = getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile.VETUMA_SAML2, "");
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(relyingParty);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "TUPAS;HST", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.INTERNAL_ERROR);
    }

    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodNotInRelyingPartyAuthMethods() throws RelyingPartyNotFoundException {
        ServiceProvider relyingParty = getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile.VETUMA_SAML2, "HST");
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(relyingParty);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "TUPAS;KATSOPWD", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.INTERNAL_ERROR);
    }

    @Test
    public void initNewSessionFailsWhenSessionProfileIsNull() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "HST", null, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "uid12345uid", "testkey", "HST", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.SESSION_INIT_FAILED);
    }

    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodIsBlank() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.VETUMA_SAML2, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        PhaseIdService phaseIdInitServiceMock = mock(PhaseIdService.class);
        when(phaseIdInitServiceMock.nextTokenId()).thenReturn("TEST_TOKEN");
        when(phaseIdInitServiceMock.newPhaseId(anyString(), anyString())).thenReturn("TEST_PHASE_ID");
        SessionHandlingService service = new SessionHandlingService(metadataServiceMock,
                vtjPersonServiceMock,
                new SessionHandlingUtils(),
                identifiedPersonBuilder,
                sessionAttributeCollector,
                uidToUserSessionsCache,
                phaseIdInitServiceMock,
                phaseIdBuiltSession);
        ProxyMessageDTO message = service.initNewSession(relyingPartyId, "uid12345uid", "testkey", " ", "logtag");
        Assert.assertEquals(ErrorType.INTERNAL_ERROR, message.getErrorType());
        Assert.assertNull(uidToUserSessionsCache.getSessionsCache().get("TEST_TOKEN"));
    }

    @Test
    public void buildNewSession() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);

        String tokenId = message.getTokenId();
        String phaseId = message.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertEquals(true, phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionInit));

        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Assert.assertNotNull(nextPhaseId);
        Assert.assertNotEquals(phaseId, nextPhaseId);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, "1234567A");
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(vtjPerson);
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // This is for testing replay prevention
        ProxyMessageDTO invalidResult = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(invalidResult);
        Assert.assertEquals(ErrorType.SESSION_BUILD_FAILED, invalidResult.getErrorType());

        tokenId = result.getTokenId();
        phaseId = result.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(true, phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepRedirectFromSP));

        nextPhaseId = phaseIdBuiltSession.newPhaseId(tokenId, stepGetSession);
        Assert.assertNotNull(nextPhaseId);

        result = sessionHandlingService.getSessionById(tokenId, nextPhaseId, "logtag");
        Assert.assertNotNull(result);
        Assert.assertEquals(convKey, result.getConversationKey());
        Assert.assertNotNull(result.getUid());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId", false, "authnRequestId");
        Assert.assertNotNull(attributes);
        Assert.assertNotEquals(0, attributes.getAttributeMap().size());
        //checking x-road attribute
        Assert.assertEquals(true, attributes.getAttributeMap().containsValue("111190-123B"));
    }

    @Test
    public void buildNewSessionWithAuthnContextClass() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);

        String tokenId = message.getTokenId();
        String phaseId = message.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertEquals(true, phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionInit));

        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Assert.assertNotNull(nextPhaseId);
        Assert.assertNotEquals(phaseId, nextPhaseId);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Class", "nordea.tupas");
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, "1234567A");
        //mocking x-road attribute
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(vtjPerson);
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // This is for testing replay prevention
        ProxyMessageDTO invalidResult = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(invalidResult);
        Assert.assertEquals(ErrorType.SESSION_BUILD_FAILED, invalidResult.getErrorType());

        tokenId = result.getTokenId();
        phaseId = result.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(true, phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepRedirectFromSP));

        nextPhaseId = phaseIdBuiltSession.newPhaseId(tokenId, stepGetSession);
        Assert.assertNotNull(nextPhaseId);

        result = sessionHandlingService.getSessionById(tokenId, nextPhaseId, "logtag");
        Assert.assertNotNull(result);
        Assert.assertEquals(convKey, result.getConversationKey());
        Assert.assertNotNull(result.getUid());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId", false, "authnRequestId");
        Assert.assertNotNull(attributes);
        Assert.assertNotEquals(0, attributes.getAttributeMap().size());
        //checking x-road attribute
        Assert.assertEquals(true, attributes.getAttributeMap().containsValue("111190-123B"));
    }

    @Test
    public void buildNewSessionFailsWithoutAuthnContextClassOrDeclaration() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);

        String tokenId = message.getTokenId();
        String phaseId = message.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertEquals(true, phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionInit));

        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Assert.assertNotNull(nextPhaseId);
        Assert.assertNotEquals(phaseId, nextPhaseId);

        Map<String, String> sessionData = new HashMap<>();
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        //mocking x-road attribute
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(vtjPerson);
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertEquals(ErrorType.INTERNAL_ERROR, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjFailsVtjNotRequired() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "TUPAS;HST", "logtag");

        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, null));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenThrow(new VtjServiceException("VTJ connection failed"));
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjFailsVtjRequired() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "TUPAS;HST", SessionProfile.VETUMA_SAML2, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.HETU, "111190-123B");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenThrow(new VtjServiceException("VTJ connection failed"));
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_FAILED, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjDataInvalid() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, null));
        VtjPerson personMock = mock(VtjPerson.class);
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(personMock);
        doThrow(InvalidVtjDataException.class).when(personMock).validate();
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        // actual test
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_INVALID, result.getErrorType());
    }
    
    @Test
    public void buildNewSessionVtjDataNotFound() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, null));

        doThrow(InvalidVtjDataException.class).when(vtjPersonServiceMock).getVtjPerson(any());
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        
        // actual test
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_INVALID, result.getErrorType());
    }
    
    @Test
    public void buildNewSessionVtjNotFoundInGetSessionAttributes() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);

        String tokenId = message.getTokenId();
        String phaseId = message.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertEquals(true, phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionInit));

        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Assert.assertNotNull(nextPhaseId);
        Assert.assertNotEquals(phaseId, nextPhaseId);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Class", "nordea.tupas");
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, "1234567A");
        //mocking x-road attribute
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(vtjPerson);
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        
        doThrow(VtjServiceException.class).when(vtjPersonServiceMock).getVtjPerson(any());
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // This is for testing replay prevention
        ProxyMessageDTO invalidResult = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(invalidResult);
        Assert.assertEquals(ErrorType.SESSION_BUILD_FAILED, invalidResult.getErrorType());

        tokenId = result.getTokenId();
        phaseId = result.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(true, phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepRedirectFromSP));

        nextPhaseId = phaseIdBuiltSession.newPhaseId(tokenId, stepGetSession);
        Assert.assertNotNull(nextPhaseId);

        result = sessionHandlingService.getSessionById(tokenId, nextPhaseId, "logtag");
        Assert.assertNotNull(result);
        Assert.assertEquals(convKey, result.getConversationKey());
        Assert.assertNotNull(result.getUid());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());
        
        doThrow(InvalidVtjDataException.class).when(vtjPersonServiceMock).getVtjPerson(any());
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId", false, "authnRequestId");
        verify(vtjPersonServiceMock, times(2)).getVtjPerson(any());
        Assert.assertNotNull(attributes);
        Assert.assertEquals(false, attributes.getAttributeMap().containsValue("111190-123B"));
    }

    @Test
    public void buildNewSessionWhenVtjIsRequiredAndKatsoIdpUsed() throws Exception {
        AuthMethod authMethod = AuthMethod.KATSOPWD;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "TUPAS;HST;KATSOPWD", SessionProfile.TUNNISTUSFI_LEGACY, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "TUPAS;HST;KATSOPWD", "logtag");
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "katso.pwd");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(new Identity(null, Identifier.Types.KID, "e12345"), null, null, null, null, null));
        verify(vtjPersonServiceMock, never()).getVtjPerson(any());
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());
    }

    @Test
    public void buildExistingSession() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "uid56789uid", convKey, "TUPAS;HST", "logtag");
        Assert.assertNotNull(message);

        String tokenId = message.getTokenId();
        String phaseId = message.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertEquals(true, phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionInit));

        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Assert.assertNotNull(nextPhaseId);
        Assert.assertNotEquals(phaseId, nextPhaseId);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        //mocking x-road attribute
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, "1234567A");
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);

        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenReturn(vtjPerson);
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // This is for testing replay prevention
        ProxyMessageDTO invalidResult = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(invalidResult);
        Assert.assertEquals(ErrorType.SESSION_BUILD_FAILED, invalidResult.getErrorType());

        tokenId = result.getTokenId();
        phaseId = result.getPhaseId();
        Assert.assertNotNull(tokenId);
        Assert.assertNotNull(phaseId);
        Assert.assertEquals(true, phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepRedirectFromSP));

        nextPhaseId = phaseIdBuiltSession.newPhaseId(tokenId, stepGetSession);
        Assert.assertNotNull(nextPhaseId);

        result = sessionHandlingService.getSessionById(tokenId, nextPhaseId, "logtag");
        Assert.assertNotNull(result);
        Assert.assertEquals(convKey, result.getConversationKey());
        Assert.assertNotNull(result.getUid());
        Assert.assertEquals("uid56789uid", result.getUid());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId", false, "authnRequestId");
        Assert.assertNotNull(attributes);
        Assert.assertNotEquals(0, attributes.getAttributeMap().size());
        //checking x-road attribute
        Assert.assertEquals(true, attributes.getAttributeMap().containsValue("111190-123B"));
    }

    @Test
    public void removeSession() throws Exception{
        String tokenId = "tokenId";
        String conversationKey = "convKey";
        SessionHandlingService service = new SessionHandlingService(metadataServiceMock,
                vtjPersonServiceMock,
                new SessionHandlingUtils(),
                identifiedPersonBuilder,
                sessionAttributeCollector,
                uidToUserSessionsCache,
                phaseIdInitSession, phaseIdBuiltSession);

        PhaseIdService phaseIdService = mock(PhaseIdService.class);
        when(phaseIdService.validateTidAndPid(anyString(), anyString())).thenReturn(true);
        when(phaseIdService.verifyPhaseId(anyString(), anyString(), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(service, "phaseIdInitSession", phaseIdService);
        ReflectionTestUtils.setField(service, "phaseIdBuiltSession", phaseIdService);

        UidToUserSessionsCache uidToUserSessionsCache = mock(UidToUserSessionsCache.class);
        Session session = new Session();
        session.setConversationKey(conversationKey);
        when(uidToUserSessionsCache.removeFromSessionCache(tokenId, AuthMethod.INIT)).thenReturn(session);
        ReflectionTestUtils.setField(service, "uidToUserSessionsCache", uidToUserSessionsCache);

        ProxyMessageDTO removeMessage = service.removeSessionById("tokenId", "phaseId", "logtag");
        Assert.assertEquals(ErrorType.NO_ERROR, removeMessage.getErrorType());
        Assert.assertNotNull(removeMessage);
        Assert.assertEquals(conversationKey, removeMessage.getConversationKey());
    }

    @Test
    public void removeSessionInvalidTidOrPid() throws Exception{
        SessionHandlingService service = new SessionHandlingService(metadataServiceMock,
                vtjPersonServiceMock,
                new SessionHandlingUtils(),
                identifiedPersonBuilder,
                sessionAttributeCollector,
                uidToUserSessionsCache,
                phaseIdInitSession, phaseIdBuiltSession);

        PhaseIdService phaseIdService = mock(PhaseIdService.class);
        when(phaseIdService.validateTidAndPid(anyString(), anyString())).thenReturn(false);
        when(phaseIdService.verifyPhaseId(anyString(), anyString(), anyString())).thenReturn(true);
        ReflectionTestUtils.setField(service, "phaseIdBuiltSession", phaseIdService);

        ProxyMessageDTO removeMessage = service.removeSessionById("tokenId", "phaseId", "logtag");
        Assert.assertNotNull(removeMessage);
        Assert.assertEquals(ErrorType.PHASE_ID_FAILED, removeMessage.getErrorType());
    }

    @Test
    public void removeSessionVerifyFailed() throws Exception{
        SessionHandlingService service = new SessionHandlingService(metadataServiceMock,
                vtjPersonServiceMock,
                new SessionHandlingUtils(),
                identifiedPersonBuilder,
                sessionAttributeCollector,
                uidToUserSessionsCache,
                phaseIdInitSession, phaseIdBuiltSession);

        PhaseIdService phaseIdService = mock(PhaseIdService.class);
        when(phaseIdService.validateTidAndPid(anyString(), anyString())).thenReturn(true);
        when(phaseIdService.verifyPhaseId(anyString(), anyString(), anyString())).thenReturn(false);
        ReflectionTestUtils.setField(service, "phaseIdBuiltSession", phaseIdService);

        ProxyMessageDTO removeMessage = service.removeSessionById("tokenId", "phaseId", "logtag");
        Assert.assertNotNull(removeMessage);
        Assert.assertEquals(ErrorType.PHASE_ID_FAILED, removeMessage.getErrorType());
    }

    @Test
    public void getVtjVerificationRequirementReturnsForbiddenForKatsoPwd() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        Assert.assertEquals(VtjVerificationRequirement.FORBIDDEN, sessionHandlingService.getVtjVerificationRequirement(relyingParty, AuthMethod.KATSOPWD));
    }

    @Test
    public void getVtjVerificationRequirementReturnsForbiddenForKatsoOtp() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        Assert.assertEquals(VtjVerificationRequirement.FORBIDDEN, sessionHandlingService.getVtjVerificationRequirement(relyingParty, AuthMethod.KATSOOTP));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMustSucceedForHst() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(false);
        Assert.assertEquals(VtjVerificationRequirement.MUST_SUCCEED, sessionHandlingService.getVtjVerificationRequirement(relyingParty, AuthMethod.HST));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMustSucceedForTupasWhenRelayingPartyReturnsRequired() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(true);
        Assert.assertEquals(VtjVerificationRequirement.MUST_SUCCEED, sessionHandlingService.getVtjVerificationRequirement(relyingParty, AuthMethod.TUPAS));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMayFailForTupasWhenRelyingPartyReturnsNotRequired() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(false);
        Assert.assertEquals(VtjVerificationRequirement.MAY_FAIL, sessionHandlingService.getVtjVerificationRequirement(relyingParty, AuthMethod.TUPAS));
    }

    @Test
    public void resolveAuthMethodFromOidResolvesTupas() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.1");
        Assert.assertEquals(AuthMethod.TUPAS, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidResolvesHst() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.2");
        Assert.assertEquals(AuthMethod.HST, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidResolvesMobiili() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.3");
        Assert.assertEquals(AuthMethod.MOBIILI, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidResolvesKatsootp() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.5");
        Assert.assertEquals(AuthMethod.KATSOOTP, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidResolvesKatsopwd() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.6");
        Assert.assertEquals(AuthMethod.KATSOPWD, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidReturnsNullWhenNotFound() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("urn:oid:1.2.246.517.3002.110.0");
        Assert.assertNull(authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidReturnsNullWhenNotUrnOid() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("1.2.246.517.3002.110.1");
        Assert.assertNull(authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidReturnsNullWhenCalledWithInitOid() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("0");
        Assert.assertNull(authMethod);
    }

    @Test
    public void getSessionAttributesReturnsNullWhenRequestedAuthMethodNotFound() throws Exception {
        Assert.assertNull(sessionHandlingService.getSessionAttributes("ANY_UID", "BAD_AUTH_METHOD_OID", "ANY_RELYING_PARTY", false, "authnRequestId"));
    }

    @Test
    public void getSessionAttributesReturnsNullWhenRelyingPartyNotFound() throws Exception {
        when(metadataServiceMock.getRelyingParty(anyString())).thenThrow(RelyingPartyNotFoundException.class);
        Assert.assertNull(sessionHandlingService.getSessionAttributes("ANY_UID", AuthMethod.HST.getOidValue(), "DOES_NOT_EXIST", false, "authnRequestId"));
    }

    private Person getMinimalValidPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setProtectionOrder(false);
        person.setDeceased(false);
        return person;
    }

    private ServiceProvider getServiceProviderWithAuthenticationMethods(String authMethods) {
        return new ServiceProvider("service-provider", "", authMethods, null, true);
    }

    private ServiceProvider getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile sessionProfile, String authMethods) {
        return new ServiceProvider("service-provider", "", authMethods, sessionProfile, true);
    }
}
