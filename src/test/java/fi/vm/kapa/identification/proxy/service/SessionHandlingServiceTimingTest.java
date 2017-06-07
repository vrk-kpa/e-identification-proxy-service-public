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
import fi.vm.kapa.identification.proxy.person.IdentifiedPersonBuilder;
import fi.vm.kapa.identification.proxy.person.GenericPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.*;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ContextConfiguration(locations = "classpath:timingTestContext.xml")
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class SessionHandlingServiceTimingTest {
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
    public void buildNewSessionWithSlowVtj() throws Exception {
        AuthMethod authMethod = AuthMethod.TUPAS;
        String convKey = "testkey";
        String relyingPartyId = "service-provider";
        ServiceProvider serviceProvider = new ServiceProvider(relyingPartyId, "", "TUPAS;HST", SessionProfile.TUNNISTUSFI_LEGACY, false);
        when(metadataServiceMock.getRelyingParty(anyString())).thenReturn(serviceProvider);
        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingPartyId, "0", convKey, "TUPAS;HST", "logtag", "authnRequestId");

        String tokenId = message.getTokenId();
        String nextPhaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionBuild);

        Map<String, String> sessionData = new HashMap<>();
        sessionData.put("AJP_Shib-AuthnContext-Decl", "nordea.tupas");
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, "1234567A");
        Person testVtjPerson = getMinimalValidPerson("111190-123B");
        Identity identity = new Identity("Testi CA", Identifier.Types.SATU, "1234567A");
        VtjPerson vtjPerson = new VtjPerson(identity, testVtjPerson);
        when(identifiedPersonBuilder.build(anyMap(), any())).thenReturn(new GenericPerson(identity, null, null, null, null, identifiers));
        // artificial wait during VTJ call
        when(vtjPersonServiceMock.getVtjPerson(any())).thenAnswer(new Answer<VtjPerson>() {
            @Override
            public VtjPerson answer(InvocationOnMock invocation) {
                try {
                    Thread.sleep(timeIntervalBuilt*2000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                return vtjPerson;
            }
        });
        when(metadataServiceMock.getAuthenticationProvider(anyString())).thenReturn(new AuthenticationProvider("", "", authMethod, "", ""));
        ProxyMessageDTO result = sessionHandlingService.buildNewSession(tokenId, nextPhaseId, sessionData, "logtag");
        Assert.assertEquals(true, phaseIdBuiltSession.verifyPhaseId(result.getPhaseId(), result.getTokenId(), stepRedirectFromSP));
    }

    private Person getMinimalValidPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setProtectionOrder(false);
        person.setDeceased(false);
        return person;
    }

}
