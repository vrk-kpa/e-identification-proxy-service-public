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
import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
import fi.vm.kapa.identification.proxy.person.GenericPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.proxy.session.UidToUserSessionsCache;
import fi.vm.kapa.identification.service.PhaseIdHistoryService;
import fi.vm.kapa.identification.service.PhaseIdService;
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.ErrorType;
import fi.vm.kapa.identification.type.Identifier;
import fi.vm.kapa.identification.type.SessionProfile;
import fi.vm.kapa.identification.vtj.model.Person;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = {"/integrationTestContext.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class SessionHandlingServiceAttibutesTest {

    @Value("${phase.id.shared.secret}")
    private String secretKey;

    @Value("${phase.id.time.interval}")
    private int timeInterval;

    @Value("${phase.id.algorithm}")
    private String hmacAlgorithm;

    @Mock
    private DummyVtjPersonService personService;

    @Mock
    private MetadataService metadataService;

    @Mock
    private PhaseIdService phaseIdService;

    @Mock
    PhaseIdHistoryService phaseIdHistoryService;

    @Autowired
    @InjectMocks
    SessionHandlingService sessionHandlingService;

    @Autowired
    UidToUserSessionsCache uidToUserSessionsCache;

    private final String convKey = "testkey";

    private final String userId = "1234567A";
    private final String issuerDn = "Testi CA";
    private final String vtjHetu = "VTJ_HETU";
    private final Identity identity = new Identity(issuerDn, Identifier.Types.SATU, userId);
    private final Person testVtjPerson = getTestPerson(vtjHetu);
    private final VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);

    private final AuthenticationProvider tupasAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", AuthMethod.TUPAS, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");
    private final AuthenticationProvider katsoAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", AuthMethod.KATSOPWD, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");
    private final AuthenticationProvider mobiiliAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", AuthMethod.MOBIILI, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");
    private final AuthenticationProvider eidasTestiAuthenticationProvider = new AuthenticationProvider("TEST_AUTH_PROVIDER", "TEST_AUTH_PROVIDER_DOMAINNAME", AuthMethod.EIDAS1, "AUTH_PROVIDER_CONTEXT_URL", "DB_ENTITY_AUTH_CONTEXT_URL");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        // Vtj Person data
        when(personService.getVtjPerson(any())).thenReturn(vtjPerson);
        // mock phaseId: not part of this test
        ReflectionTestUtils.setField(sessionHandlingService, "phaseIdInitSession", phaseIdService);
        ReflectionTestUtils.setField(sessionHandlingService, "phaseIdBuiltSession", phaseIdService);
        when(phaseIdService.verifyPhaseId(anyString(), anyString(), anyString())).thenReturn(true);
        when(phaseIdService.nextTokenId()).thenReturn("1", "2", "3", "4");
        when(phaseIdService.newPhaseId(anyString(), anyString())).thenReturn("1", "2", "3", "4");
        when(phaseIdHistoryService.areIdsConsumed(anyString(), anyString())).thenReturn(false);
    }

    @After
    public void tearDown() {
        Mockito.reset(metadataService, personService, phaseIdHistoryService, phaseIdService);
        uidToUserSessionsCache.getSessionsCache().clear();
    }

    @Test
    public void initSession() throws Exception {
        String relyingPartyId = "relyingParty";
        // set metadata
        AuthMethod authMethod = AuthMethod.TUPAS;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(tupasAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        assertEquals(serviceProvider.getLevelOfAssurance(), initResponse.getLoaProfile());
        assertEquals(requestedAuthMethods, initResponse.getAuthenticationMethods());
    }

    @Test
    public void createFullSessionTunnistusFiLegacyWithProtectionOrder() throws Exception {
        String relyingPartyId = "relyingParty";
        // set metadata
        AuthMethod authMethod = AuthMethod.TUPAS;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(tupasAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        VtjPerson testVtjPersonWithProtectionOrder = new VtjPerson(new Identity(null, Identifier.Types.HETU, vtjHetu), getTestPersonWithProtectionOrder(vtjHetu));
        when(personService.getVtjPerson(any())).thenReturn(testVtjPersonWithProtectionOrder);

        // SP session data
        Map<String,String> sessionData = getMyspNordeaSessionData(Identifier.Types.HETU, userId, "SP_HETU", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "true");
        expected.put("vtjRequired", "false");
        expected.put("provider", "AUTH_PROVIDER_CONTEXT_URL");
        expected.put("legacyPersonName", "TESTAA PORTAALIA");
        expected.put("legacyPin", "SP_HETU");
        expected.put("version", "legacyvalue");
        expected.put("samlElectronicIdentificationNumber", "VTJ_SATU");
        expected.put("samlNationalIdentificationNumber", vtjHetu);
        expected.put("samlFirstName", "VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDisplayName", "VTJ_NICK VTJ_SUKUNIMI");
        expected.put("samlSn", "VTJ_SUKUNIMI");
        expected.put("samlGivenName", "VTJ_NICK");
        expected.put("samlCn", "VTJ_SUKUNIMI VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDomesticAddress", "VTJ_DOMESTIC_ADDRESS_S");
        expected.put("samlCity", "VTJ_CITY_S");
        expected.put("samlPostalCode", "VTJ_POSTAL_CODE");
        expected.put("samlState", "VTJ_STATE_CODE");
        expected.put("samlMunicipality", "VTJ_MUNICIPALITY_S");
        expected.put("samlMunicipalityCode", "VTJ_MUNICIPALITY_CODE");
        expected.put("samlTemporaryCity", "VTJ_TEMPORARY_CITY_S");
        expected.put("samlTemporaryDomesticAddress", "VTJ_TEMPORARY_DOMESTIC_ADDRESS_S");
        expected.put("samlTemporaryPostalCode", "VTJ_TEMPORARY_POSTAL_CODE");
        expected.put("samlForeignAddress", "VTJ_FOREIGN_ADDRESS");
        expected.put("samlForeignLocalityAndState", "VTJ_FOREIGN_LOCALITY_AND_STATE_S");
        expected.put("samlForeignLocalityAndStateClearText", "VTJ_FOREIGN_LOCALITY_AND_STATE_CLEARTEXT");
        expected.put("samlMail", "VTJ_EMAIL");
        expected.put("samlProtectionOrder", "1");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionVetumaSaml2() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.TUPAS;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(tupasAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_SAML2, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getMyspNordeaSessionData(Identifier.Types.HETU, userId, "SP_HETU", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "true");
        expected.put("vtjRequired", "false");
        expected.put("provider", "DB_ENTITY_AUTH_CONTEXT_URL");
        expected.put("samlElectronicIdentificationNumber", "VTJ_SATU");
        expected.put("samlNationalIdentificationNumber", vtjHetu);
        expected.put("samlFirstName", "VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDisplayName", "VTJ_NICK VTJ_SUKUNIMI");
        expected.put("samlSn", "VTJ_SUKUNIMI");
        expected.put("samlGivenName", "VTJ_NICK");
        expected.put("samlCn", "VTJ_SUKUNIMI VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDomesticAddress", "VTJ_DOMESTIC_ADDRESS_S");
        expected.put("samlCity", "VTJ_CITY_S");
        expected.put("samlPostalCode", "VTJ_POSTAL_CODE");
        expected.put("samlState", "VTJ_STATE_CODE");
        expected.put("samlMunicipality", "VTJ_MUNICIPALITY_S");
        expected.put("samlMunicipalityCode", "VTJ_MUNICIPALITY_CODE");
        expected.put("samlTemporaryCity", "VTJ_TEMPORARY_CITY_S");
        expected.put("samlTemporaryDomesticAddress", "VTJ_TEMPORARY_DOMESTIC_ADDRESS_S");
        expected.put("samlTemporaryPostalCode", "VTJ_TEMPORARY_POSTAL_CODE");
        expected.put("samlForeignAddress", "VTJ_FOREIGN_ADDRESS");
        expected.put("samlForeignLocalityAndState", "VTJ_FOREIGN_LOCALITY_AND_STATE_S");
        expected.put("samlForeignLocalityAndStateClearText", "VTJ_FOREIGN_LOCALITY_AND_STATE_CLEARTEXT");
        expected.put("samlMail", "VTJ_EMAIL");
        expected.put("samlProtectionOrder", "0");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionVetumaLegacy() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.TUPAS;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(tupasAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_LEGACY, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getMyspNordeaSessionData(Identifier.Types.HETU, userId, "SP_HETU", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "true");
        expected.put("vtjRequired", "false");
        expected.put("samlElectronicIdentificationNumber", "VTJ_SATU");
        expected.put("samlNationalIdentificationNumber", vtjHetu);
        expected.put("samlFirstName", "VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDisplayName", "VTJ_NICK VTJ_SUKUNIMI");
        expected.put("samlSn", "VTJ_SUKUNIMI");
        expected.put("samlGivenName", "VTJ_NICK");
        expected.put("samlCn", "VTJ_SUKUNIMI VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDomesticAddress", "VTJ_DOMESTIC_ADDRESS_S");
        expected.put("samlCity", "VTJ_CITY_S");
        expected.put("samlPostalCode", "VTJ_POSTAL_CODE");
        expected.put("samlState", "VTJ_STATE_CODE");
        expected.put("samlMunicipality", "VTJ_MUNICIPALITY_S");
        expected.put("samlMunicipalityCode", "VTJ_MUNICIPALITY_CODE");
        expected.put("samlTemporaryCity", "VTJ_TEMPORARY_CITY_S");
        expected.put("samlTemporaryDomesticAddress", "VTJ_TEMPORARY_DOMESTIC_ADDRESS_S");
        expected.put("samlTemporaryPostalCode", "VTJ_TEMPORARY_POSTAL_CODE");
        expected.put("samlForeignAddress", "VTJ_FOREIGN_ADDRESS");
        expected.put("samlForeignLocalityAndState", "VTJ_FOREIGN_LOCALITY_AND_STATE_S");
        expected.put("samlForeignLocalityAndStateClearText", "VTJ_FOREIGN_LOCALITY_AND_STATE_CLEARTEXT");
        expected.put("samlMail", "VTJ_EMAIL");
        expected.put("samlProtectionOrder", "0");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionShibKatsopwdVetumaLegacy() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.KATSOPWD;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(katsoAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_LEGACY, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getShibSpKatsopwdSessionData(Identifier.Types.SATU, "ap76i8", "NAME FROM SP", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "false");
        expected.put("vtjRequired", "false");
        expected.put("samlCn", "NAME FROM SP");
        expected.put("samlKid", "ap76i8");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionShibKatsopwdTunnistusLegacy() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.KATSOPWD;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(katsoAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getShibSpKatsopwdSessionData(Identifier.Types.SATU, "ap76i8", "NAME FROM SP", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "false");
        expected.put("vtjRequired", "false");
        expected.put("samlCn", "NAME FROM SP");
        expected.put("samlKid", "ap76i8");
        expected.put("provider", "AUTH_PROVIDER_CONTEXT_URL");
        expected.put("legacyVersion", "katso-1.1");
        expected.put("legacyKid", "ap76i8");
        expected.put("legacyPersonName", "NAME FROM SP");
        expected.put("version", "legacyvalue");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionShibMobiilivarmenneSaml2() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.MOBIILI;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(mobiiliAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_SAML2, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getMobiilivarmenneSessionData(Identifier.Types.SATU, "99999578U", "SP_HETU", "SP_MOBILE", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "true");
        expected.put("vtjRequired", "false");
        expected.put("provider", "DB_ENTITY_AUTH_CONTEXT_URL");
        expected.put("samlElectronicIdentificationNumber", "VTJ_SATU");
        expected.put("samlNationalIdentificationNumber", vtjHetu);
        expected.put("samlFirstName", "VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDisplayName", "VTJ_NICK VTJ_SUKUNIMI");
        expected.put("samlSn", "VTJ_SUKUNIMI");
        expected.put("samlGivenName", "VTJ_NICK");
        expected.put("samlCn", "VTJ_SUKUNIMI VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        expected.put("samlDomesticAddress", "VTJ_DOMESTIC_ADDRESS_S");
        expected.put("samlCity", "VTJ_CITY_S");
        expected.put("samlPostalCode", "VTJ_POSTAL_CODE");
        expected.put("samlState", "VTJ_STATE_CODE");
        expected.put("samlMunicipality", "VTJ_MUNICIPALITY_S");
        expected.put("samlMunicipalityCode", "VTJ_MUNICIPALITY_CODE");
        expected.put("samlTemporaryCity", "VTJ_TEMPORARY_CITY_S");
        expected.put("samlTemporaryDomesticAddress", "VTJ_TEMPORARY_DOMESTIC_ADDRESS_S");
        expected.put("samlTemporaryPostalCode", "VTJ_TEMPORARY_POSTAL_CODE");
        expected.put("samlForeignAddress", "VTJ_FOREIGN_ADDRESS");
        expected.put("samlForeignLocalityAndState", "VTJ_FOREIGN_LOCALITY_AND_STATE_S");
        expected.put("samlForeignLocalityAndStateClearText", "VTJ_FOREIGN_LOCALITY_AND_STATE_CLEARTEXT");
        expected.put("samlMail", "VTJ_EMAIL");
        expected.put("samlMobile", "SP_MOBILE");
        expected.put("samlProtectionOrder", "0");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    @Test
    public void createFullSessionShibMobiilivarmenneSaml2VtjNotAvailable() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.MOBIILI;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(mobiiliAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_SAML2, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);
        // Vtj not available
        when(personService.getVtjPerson(any(GenericPerson.class))).thenThrow(VtjServiceException.class);
        // SP session data
        Map<String,String> sessionData = getMobiilivarmenneSessionData(Identifier.Types.SATU, "99999578U", "SP_HETU", "SP_MOBILE", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "false");
        expected.put("vtjRequired", "false");
        expected.put("provider", "DB_ENTITY_AUTH_CONTEXT_URL");
        expected.put("samlElectronicIdentificationNumber", "99999578U");
        expected.put("samlNationalIdentificationNumber", "SP_HETU");
        expected.put("samlSn", "SP_SURNAME");
        expected.put("samlMobile", "SP_MOBILE");
        expected.put("samlDisplayName", "Raija SP_SURNAME");
        expected.put("samlGivenName", "Raija Talvikki");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }


    @Test
    public void createFullSessionEidasTesti() throws Exception {
        String relyingPartyId = "relyingParty";

        // set metadata
        AuthMethod authMethod = AuthMethod.EIDAS1;
        when(metadataService.getAuthenticationProvider("AUTH_PROVIDER_CONTEXT_URL")).thenReturn(eidasTestiAuthenticationProvider);
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "LOA", authMethod.name(), SessionProfile.VETUMA_SAML2, false);
        when(metadataService.getRelyingParty(anyString())).thenReturn(serviceProvider);

        // SP session data
        Map<String,String> sessionData = getEidasTestiSessionData(Identifier.Types.EIDAS_ID, "FR/ES/1234567", "AUTH_PROVIDER_CONTEXT_URL");

        // actual test
        String requestedAuthMethods = authMethod.name();
        ProxyMessageDTO initResponse = sessionHandlingService.initNewSession(serviceProvider.getEntityId(), "0", convKey, requestedAuthMethods, "logtag");
        ProxyMessageDTO buildResponse = sessionHandlingService.buildNewSession(initResponse.getTokenId(), "1", sessionData, "logtag");
        ProxyMessageDTO result = sessionHandlingService.getSessionById(buildResponse.getTokenId(), "2", "logtag");
        assertEquals(convKey, result.getConversationKey());
        assertEquals(authMethod.name(), result.getUsedAuthenticationMethod());
        assertNotNull(result.getUid());
        assertEquals(ErrorType.NO_ERROR, result.getErrorType());

        // get attributes
        SessionAttributeDTO attributes = sessionHandlingService.getSessionAttributes(result.getUid(), authMethod.getOidValue(), "testEntityId");
        assertNotNull(attributes);

        Map<String,String> attributeMap = attributes.getAttributeMap();
        Map<String,String> expected = new HashMap<>();
        expected.put("vtjInvalid", "false");
        expected.put("vtjVerified", "false");
        expected.put("vtjRequired", "false");
        expected.put("provider", "DB_ENTITY_AUTH_CONTEXT_URL");
        expected.put("samlFirstName", "GIVEN_NAME");
        expected.put("samlFamilyName", "FAMILY_NAME");
        expected.put("samlDateOfBirth", "1999-12-31");
        expected.put("samlUid", "FR/ES/1234567");
        assertThat(attributeMap.entrySet(), equalTo(expected.entrySet()));
    }

    private Map<String,String> getEidasTestiSessionData(Identifier.Types identifierType, String identifier, String auth_provider_context_url) {
        Map<String,String> sessionData = new HashMap<>();
        sessionData.put("identifierType", identifierType.name());
        sessionData.put("AJP_eidasPersonIdentifier", identifier);
        sessionData.put("AJP_Shib-AuthnContext-Decl", auth_provider_context_url);

        sessionData.put("AJP_eidasGivenName", "GIVEN_NAME");
        sessionData.put("AJP_eidasFamilyName", "FAMILY_NAME");
        sessionData.put("AJP_eidasDateOfBirth", "1999-12-31");
        // these are typical HTTP headers sent in session data
        return sessionData;

    }

    private Person getTestPersonWithProtectionOrder(String hetu) {
        Person person = getTestPerson(hetu);
        person.setProtectionOrder(true);
        return person;
    }

    private Person getTestPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setSatu("VTJ_SATU");
        person.setSatuValid(true);

        person.setProtectionOrder(false);
        person.setDeceased(false);
        person.setFirstNames("VTJ_EKA VTJ_TOKA VTJ_KOLMAS");
        person.setNickName("VTJ_NICK");
        person.setEmailAddress("VTJ_EMAIL");
        person.setLastName("VTJ_SUKUNIMI");
        person.setMunicipalityCode("VTJ_MUNICIPALITY_CODE");
        person.setPostalCode("VTJ_POSTAL_CODE");
        person.setStateCode("VTJ_STATE_CODE");
        person.setTemporaryPostalCode("VTJ_TEMPORARY_POSTAL_CODE");
        person.setForeignAddress("VTJ_FOREIGN_ADDRESS");
        person.setForeignLocalityAndStateClearText("VTJ_FOREIGN_LOCALITY_AND_STATE_CLEARTEXT");

        person.setCityR("VTJ_CITY_R");
        person.setMunicipalityR("VTJ_MUNICIPALITY_R");
        person.setDomesticAddressR("VTJ_DOMESTIC_ADDRESS_R");
        person.setTemporaryCityR("VTJ_TEMPORARY_CITY_R");
        person.setTemporaryDomesticAddressR("VTJ_TEMPORARY_DOMESTIC_ADDRESS_R");
        person.setForeignLocalityAndStateR("VTJ_FOREIGN_LOCALITY_AND_STATE_R");

        person.setCityS("VTJ_CITY_S");
        person.setMunicipalityS("VTJ_MUNICIPALITY_S");
        person.setDomesticAddressS("VTJ_DOMESTIC_ADDRESS_S");
        person.setTemporaryCityS("VTJ_TEMPORARY_CITY_S");
        person.setTemporaryDomesticAddressS("VTJ_TEMPORARY_DOMESTIC_ADDRESS_S");
        person.setForeignLocalityAndStateS("VTJ_FOREIGN_LOCALITY_AND_STATE_S");

        return person;
    }

    private Map<String,String> getMyspNordeaSessionData(Identifier.Types identifierType, String userId, String hetu, String authContextDecl) {
        Map<String,String> sessionData = new HashMap<>();
        sessionData.put("identifierType", identifierType.name());
        sessionData.put("REMOTE_USER", userId);
        sessionData.put("AJP_Shib-AuthnContext-Decl", authContextDecl);

        sessionData.put("AJP_cn", "TESTAA PORTAALIA");
        sessionData.put("AJP_hetu", hetu);
        sessionData.put("AJP_Shib-Application-ID", "default");
        sessionData.put("AJP_Shib-Authentication-Instant", "2016-08-16T14:01:39.304Z");
        sessionData.put("AJP_Shib-Identity-Provider", "https://www.tunnistus-dev.xyz/tupasidp2");
        sessionData.put("AJP_Shib-Session-Index", "_123456789abcdef");
        sessionData.put("AJP_Shib-Session-ID", "_123456789abcdef01234");
        // these are typical HTTP headers sent in session data
        sessionData.put("DNT", "1");
        sessionData.put("Upgrade-Insecure-Requests", "1");
        sessionData.put("referer", "https://www.tunnistus-dev.xyz/tupasidp/profile/SAML2/Redirect/SSO?execution=e2s1&_eventId_proceed=1");
        sessionData.put("accept-language", "fi,en-US;q=0.8,en;q=0.6");
        sessionData.put("cookie", "E-Identification-Lang=fi; _shibsession_123456789abcdef=_0123456789abcdef0123456789abcdef");
        sessionData.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        sessionData.put("Cache-Control", "max-age=0");
        sessionData.put("host", "tun-dev-sp1:8030");
        sessionData.put("connection", "close");
        sessionData.put("accept-encoding", "gzip, deflate, sdch, br");
        sessionData.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Safari/537.36");
        return sessionData;
    }

    private Map<String,String> getShibSpKatsopwdSessionData(Identifier.Types identifierType, String userId, String name, String authContextDecl) {
        Map<String,String> sessionData = new HashMap<>();
        sessionData.put("identifierType", identifierType.name());
        sessionData.put("REMOTE_USER", userId);
        sessionData.put("AJP_Shib-AuthnContext-Decl", authContextDecl);
        sessionData.put("AJP_tfiPersonName", name);
        sessionData.put("identifierType", "KID");
        sessionData.put("AJP_Shib-Application-ID", "default");
        sessionData.put("AJP_Shib-Authentication-Instant", "2016-08-19T13:01:02.115Z");
        sessionData.put("AJP_Shib-Identity-Provider", "https://htesti.katso.tunnistus.fi/uas");
        sessionData.put("AJP_Shib-Session-Index", "_059dc2f337573f80468843f54379299fff1a0aaa");
        sessionData.put("AJP_Shib-Session-ID", "_42bdb9e587ffe160bf8b0f0b162e4db6");
        sessionData.put("AJP_tfiKid", "ap76i8");
        sessionData.put("AJP_tfiVersion", "katso-1.1");
        return sessionData;
    }

    private Map<String,String> getMobiilivarmenneSessionData(Identifier.Types identifierType, String userId, String hetu, String mobileNumber, String authContextClass) {
        Map<String,String> sessionData = new HashMap<>();
        sessionData.put("identifierType", identifierType.name());
        sessionData.put("REMOTE_USER", userId);
        sessionData.put("AJP_mobileNumber", mobileNumber);
        sessionData.put("AJP_hetu", hetu);
        sessionData.put("AJP_sn", "SP_SURNAME");
        sessionData.put("AJP_givenName", "Raija Talvikki");
        sessionData.put("AJP_Shib-Application-ID", "default");
        sessionData.put("AJP_Shib-Authentication-Instant", "2016-08-22T08:43:59.535Z");
        sessionData.put("AJP_Shib-Authentication-Method", "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract");
        sessionData.put("AJP_Shib-AuthnContext-Class", authContextClass);
        sessionData.put("AJP_Shib-Identity-Provider", "https://test-mobile.apro.tunnistus.fi/idp/shibboleth");
        sessionData.put("AJP_Shib-Session-Index", "_1b61ef21b65b3603e5a383f314687944");
        sessionData.put("AJP_Shib-Session-ID", "_1344b796d8b5ffe40ccd44b267495bf2");
        sessionData.put("AJP_satu", "99999578U");
        return sessionData;
    }

    private Map<String,String> getHstSessionData(String userId) {
        Map<String,String> sessionData = new HashMap<>();
        sessionData.put("AJP_satu", userId); // "99900890B"
        sessionData.put("identifierType", "SATU");
        sessionData.put("AJP_Shib-Session-Index", "_80965ae5ebfc9bad7f43cd153fce0e1d");
        sessionData.put("AJP_Shib-Identity-Provider", "https://kortti.tunnistus-dev.xyz/hstidp/idp1");
        sessionData.put("AJP_Shib-Authentication-Method", "https://www.tunnistus.fi/ubitp/saml2/names/ac/pki.hst.3");
        sessionData.put("AJP_Shib-Application-ID", "default");
        sessionData.put("AJP_Shib-AuthnContext-Class", "https://www.tunnistus.fi/ubitp/saml2/names/ac/pki.hst.3");
        sessionData.put("AJP_Shib-Session-ID", "_5da6a3315550337b9044180bd7055e28");
        sessionData.put("AJP_Shib-Authentication-Instant", "2016-09-09T07:16:44.397Z");
        sessionData.put("REMOTE_USER", userId);
        sessionData.put("referer", "https://kortti.tunnistus-dev.xyz/hstidp/profile/SAML2/Redirect/SSO?execution=e2s1&_eventId_proceed=1");
        sessionData.put("Upgrade-Insecure-Requests", "1");
        sessionData.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        sessionData.put("host", "tun-dev-sp1:8030");
        sessionData.put("connection", "close");
        sessionData.put("accept-encoding", "gzip, deflate, br");
        sessionData.put("accept-language", "en-US,en;q=0.5");
        sessionData.put("cookie", "E-Identification-Lang=fi;_shibsession_64656661756c7468747470733a2f2f7777772e74756e6e69737475732d6465762e78797a2f737032\u003d_5da6a3315550337b9044180bd7055e28");
        sessionData.put("user-agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0");
        return sessionData;
    }
}
