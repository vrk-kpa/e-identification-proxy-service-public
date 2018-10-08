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
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

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

    @Spy
    @Autowired
    private SessionAttributeCollector sessionAttributeCollector;

    @Autowired
    @InjectMocks
    private SessionHandlingService sessionHandlingService;

    private PhaseIdService phaseIdInitSession;
    private PhaseIdService phaseIdBuiltSession;

    private final List<AuthenticationProvider> providers = new ArrayList<>();
    private final AuthenticationProvider tupasAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", "TUPAS", AuthMethod.fLoA2, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");
    private final AuthenticationProvider hstAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", "HST", AuthMethod.fLoA3, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");
    private final AuthenticationProvider katsoAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", "KATSOPWD", AuthMethod.KATSOPWD, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        phaseIdInitSession = new PhaseIdService(secretKey, timeInterval, hmacAlgorithm);
        phaseIdBuiltSession = new PhaseIdService(secretKey, timeIntervalBuilt, hmacAlgorithm);
        providers.add(tupasAuthenticationProvider);
        providers.add(hstAuthenticationProvider);
        providers.add(katsoAuthenticationProvider);
    }

    @Test
    public void initializeNewSession() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider("service-provider", "", "fLoA2;fLoA3", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "0", "testkey", "fLoA2;fLoA3", "logtag");
        Assert.assertNotNull(message);
        Assert.assertNotNull(message.getTokenId());
        Assert.assertNotNull(message.getPhaseId());
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
        Assert.assertTrue(phaseIdInitSession.verifyPhaseId(message.getPhaseId(), message.getTokenId(), stepSessionInit));

    }

    @Test
    public void initExistingSession() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider("service-provider", "", "fLoA2;fLoA3", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "fLoA2;fLoA3", "logtag");
        Assert.assertNotNull(message);
        Assert.assertNotNull(message.getTokenId());
        Assert.assertNotNull(message.getPhaseId());
        Assert.assertEquals(ErrorType.NO_ERROR, message.getErrorType());
    }

    @Test
    public void initNewSessionFailsWhenRelyingPartyAuthenticationMethodIsBlank() throws RelyingPartyNotFoundException {
        ServiceProvider relyingParty = getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile.VETUMA_SAML2, "");
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(relyingParty);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "fLoA2;fLoA3", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.INTERNAL_ERROR);
    }

    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodNotInRelyingPartyAuthMethods() throws RelyingPartyNotFoundException {
        ServiceProvider relyingParty = getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile.VETUMA_SAML2, "fLoA3");
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(relyingParty);
        ProxyMessageDTO message = sessionHandlingService.initNewSession("service-provider", "uid12345uid", "testkey", "fLoA2;KATSOPWD", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.INTERNAL_ERROR);
    }

    @Test
    public void initNewSessionFailsWhenSessionProfileIsNull() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA3", null, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "uid12345uid", "testkey", "fLoA3", "logtag");
        Assert.assertTrue(message.getErrorType() == ErrorType.SESSION_INIT_FAILED);
    }

    // For fLoA2 request, fLoA3 should always be added and fLoA2+fLoA3 methods returned
    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodIsBlank_LoA2() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
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
    /* Check after EIDAS LoA implementation..
    @Test
    public void initNewSessionFailsWhenExpandedAuthMethodStringIsEmpty() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "LOA1;fLoA2", SessionProfile.VETUMA_SAML2, true);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
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
        ProxyMessageDTO message = service.initNewSession(relyingPartyId, "uid12345uid", "testkey", "LOA1", "logtag");
        Assert.assertEquals(ErrorType.SESSION_INIT_FAILED, message.getErrorType());
    }
    */
    // For fLoA3 request, only fLoA3 methods returned
    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodIsBlank() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA3", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
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

    // For "special" methods, only those should be returned
    @Test
    public void initNewSessionFailsWhenRequestedAuthenticationMethodIsBlank_Katso() throws Exception {
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "KATSOPWD", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
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
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
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
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
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
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertEquals(ErrorType.INTERNAL_ERROR, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjFailsVtjNotRequired() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        // attribute map needed in SessionHandlingService.getVtjVerificationRequirement()
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        attrs.put("samlCn", "cn");
        Mockito.doReturn(attrs).when(sessionAttributeCollector).getAttributes(any());
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "fLoA2;fLoA3", "logtag");

        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, null));
        when(vtjPersonServiceMock.getVtjPerson(any())).thenThrow(new VtjServiceException("VTJ connection failed"));
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjFailsVtjRequired() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "fLoA2;fLoA3", SessionProfile.VETUMA_SAML2, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        // attribute map needed in SessionHandlingService.getVtjVerificationRequirement()
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        Mockito.doReturn(attrs).when(sessionAttributeCollector).getAttributes(any());

        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_FAILED, result.getErrorType());
    }

    @Test
    public void buildNewSessionVtjDataInvalid() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        // attribute map needed in SessionHandlingService.getVtjVerificationRequirement()
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        Mockito.doReturn(attrs).when(sessionAttributeCollector).getAttributes(any());

        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
        // actual test
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_INVALID, result.getErrorType());
    }
    
    @Test
    public void buildNewSessionVtjDataNotFound() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "fLoA2;fLoA3", "logtag");
        Assert.assertNotNull(message);
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Identity identity = new Identity("", Identifier.Types.HETU, "111190-123B");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));

        doThrow(InvalidVtjDataException.class).when(vtjPersonServiceMock).getVtjPerson(any());
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "TUPAS", authMethod, "", ""));
        
        // actual test
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.VTJ_INVALID, result.getErrorType());
    }

    @Test
    public void buildNewSessionFailsWhenEidasMethodAndServiceProviderSupportNone() throws Exception {
        AuthMethod authMethod = AuthMethod.EIDAS_LOA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3;EIDAS_LOA2", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.none, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3;EIDAS_LOA2", "logtag");
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
        sessionData.put("AJP_Shib-AuthnContext-Class", "eidas.idp.classref");

        //mocking x-road attribute
        Identity identity = new Identity("Testi CA", Identifier.Types.EIDAS_ID, "eid");
        doThrow(VtjServiceException.class).when(vtjPersonServiceMock).getVtjPerson(any());

        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.EIDAS_ID, "eid");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("Eidas", "", "EIDAS_LOA2", authMethod, "http://eidas.europa.eu/LoA/substantial", "urn:oid:1.2.246.517.3002.110.997"));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertEquals(ErrorType.INTERNAL_ERROR, result.getErrorType());
    }
    
    @Test
    public void buildNewSessionWhenVtjNotFoundThenGetSessionAttributesReturnsVtjInvalid() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3", "logtag");
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
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.HETU, "111190-123B");
        //mocking x-road attribute
        Identity identity = new Identity("Testi CA", Identifier.Types.HETU, "111190-123B");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, "common_name", null, null, null, identifiers));
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "TUPAS", authMethod, "", ""));
        
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
        Assert.assertEquals("true", attributes.getAttributeMap().get("vtjInvalid"));
    }

    @Test
    public void buildNewSessionWhenVtjIsRequiredAndKatsoIdpUsed() throws Exception {
        AuthMethod authMethod = AuthMethod.KATSOPWD;
        String convKey = "testkey";
        String entityId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(entityId, "", "fLoA2;fLoA3;KATSOPWD", SessionProfile.TUNNISTUSFI_LEGACY, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        // attribute map needed in SessionHandlingService.getVtjVerificationRequirement()
        Map<String,String> attrs = new HashMap<>(); // no hetu, no satu (katso) => FORBIDDEN
        Mockito.doReturn(attrs).when(sessionAttributeCollector).getAttributes(any());

        ProxyMessageDTO message = sessionHandlingService.initNewSession(entityId, "0", convKey, "fLoA2;fLoA3;KATSOPWD", "logtag");
        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);
        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "katso.pwd");
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(new Identity(null, Identifier.Types.KID, "e12345"), null, null, null, null, null));
        verify(vtjPersonServiceMock, never()).getVtjPerson(any());
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertNotNull(result);
        Assert.assertNotEquals(tokenId, result.getTokenId());
        Assert.assertEquals(ErrorType.NO_ERROR, result.getErrorType());
    }

    @Test
    public void buildExistingSession() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA2;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, true,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "uid56789uid", convKey, "fLoA2;fLoA3", "logtag");
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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "", authMethod, "", ""));
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

        // For fLoA2, check that we only have one entry in session
        Assert.assertEquals(1, uidToUserSessionsCache.getSessionDTOMapByKey(result.getUid()).size());

        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId", false, "authnRequestId");
        Assert.assertNotNull(attributes);
        Assert.assertNotEquals(0, attributes.getAttributeMap().size());
        //checking x-road attribute
        Assert.assertEquals(true, attributes.getAttributeMap().containsValue("111190-123B"));
    }

    @Test
    public void identificationWithLoA3CreatesLoA2Session() throws Exception {
        AuthMethod authMethod = AuthMethod.fLoA3;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "fLoA2;fLoA3", SessionProfile.TUNNISTUSFI_LEGACY, false,  "",EidasSupport.full, null);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        when(metadataServiceMock.getAuthenticationProviders()).thenReturn(new MetadataService.ApprovedAuthenticationProviders(providers));

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "fLoA2;fLoA3", "logtag");
        Assert.assertNotNull(message);
        Assert.assertTrue(uidToUserSessionsCache.getSessionDTOMapByKey(message.getTokenId()).containsKey(AuthMethod.INIT));
        // 1. at this stage the can be only one session
        Assert.assertEquals(1, uidToUserSessionsCache.getSessionDTOMapByKey(message.getTokenId()).size());

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
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", "HST", authMethod, "", ""));
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
        // 2. finalising fLoA3 session generates fLoA2 session
        Assert.assertEquals(2, uidToUserSessionsCache.getSessionDTOMapByKey(result.getUid()).size());

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
    public void getVtjVerificationRequirementReturnsForbiddenForNoHetuNoSatu() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        Session session = new Session();
        Map<String,String> attrs = new HashMap<>(); // no hetu, no satu
        when(sessionAttributeCollector.getAttributes(session)).thenReturn(attrs);
        Assert.assertEquals(VtjVerificationRequirement.FORBIDDEN, sessionHandlingService.getVtjVerificationRequirement(relyingParty, session, AuthMethod.KATSOOTP));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMustSucceedForSatu() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(false);
        Session session = new Session();
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlElectronicIdentificationNumber", "satu");
        when(sessionAttributeCollector.getAttributes(session)).thenReturn(attrs);
        Assert.assertEquals(VtjVerificationRequirement.MUST_SUCCEED, sessionHandlingService.getVtjVerificationRequirement(relyingParty, session, AuthMethod.fLoA3));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMustSucceedForHetuNoCn() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(false);
        Session session = new Session();
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        when(sessionAttributeCollector.getAttributes(session)).thenReturn(attrs);
        Assert.assertEquals(VtjVerificationRequirement.MUST_SUCCEED, sessionHandlingService.getVtjVerificationRequirement(relyingParty, session, AuthMethod.fLoA2));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMustSucceedWhenRelyingPartyReturnsRequired() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(true);
        Session session = new Session();
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        attrs.put("samlCn", "cn");
        when(sessionAttributeCollector.getAttributes(session)).thenReturn(attrs);
        Assert.assertEquals(VtjVerificationRequirement.MUST_SUCCEED, sessionHandlingService.getVtjVerificationRequirement(relyingParty, session, AuthMethod.fLoA2));
    }

    @Test
    public void getVtjVerificationRequirementReturnsMayFailWhenRelyingPartyReturnsNotRequired() throws Exception {
        ServiceProvider relyingParty = mock(ServiceProvider.class);
        when(relyingParty.isVtjVerificationRequired()).thenReturn(false);
        Session session = new Session();
        Map<String,String> attrs = new HashMap<>();
        attrs.put("samlNationalIdentificationNumber", "hetu");
        attrs.put("samlCn", "cn");
        when(sessionAttributeCollector.getAttributes(session)).thenReturn(attrs);
        Assert.assertEquals(VtjVerificationRequirement.MAY_FAIL, sessionHandlingService.getVtjVerificationRequirement(relyingParty, session, AuthMethod.fLoA2));
    }

    /* Check after EIDAS LoA implementation..
    @Test
    public void resolveAuthMethodFromOidResolvesLoa1() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("http://eidas.europa.eu/LoA/low");
        Assert.assertEquals(AuthMethod.LOA1, authMethod);
    }
    */

    @Test
    public void resolveAuthMethodFromOidResolvesLoa2() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("http://ftn.ficora.fi/2017/loa2");
        Assert.assertEquals(AuthMethod.fLoA2, authMethod);
    }

    @Test
    public void resolveAuthMethodFromOidResolvesLoa3() throws Exception {
        AuthMethod authMethod = sessionHandlingService.resolveAuthMethodFromOid("http://ftn.ficora.fi/2017/loa3");
        Assert.assertEquals(AuthMethod.fLoA3, authMethod);
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
    public void resolveAuthMethodFromOidReturnsNullWhenNotUrnOidOrHttp() throws Exception {
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
        Assert.assertNull(sessionHandlingService.getSessionAttributes("ANY_UID", AuthMethod.fLoA3.getOidValue(), "DOES_NOT_EXIST", false, "authnRequestId"));
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
        return new ServiceProvider("service-provider", "", authMethods, null, true,  "",EidasSupport.full, null);
    }

    private ServiceProvider getServiceProviderWithSessionProfileAndAuthenticationMethods(SessionProfile sessionProfile, String authMethods) {
        return new ServiceProvider("service-provider", "", authMethods, sessionProfile, true,  "",EidasSupport.full, null);
    }
}
