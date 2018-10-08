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
package fi.vm.kapa.identification.proxy.metadata;

import fi.vm.kapa.identification.dto.MetadataDTO;
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.ProviderType;
import fi.vm.kapa.identification.type.SessionProfile;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

public class MetadataClientTest {

    @Autowired
    @InjectMocks
    MetadataClient metadataClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(metadataClient, "metadataServerUrl", "http://example.com");
    }

    @Test
    public void getServiceProviders() throws Exception {
        MetadataClient metadataClientSpy = Mockito.spy(metadataClient);
        MetadataDTO serviceProvider = new MetadataDTO();
        serviceProvider.setEntityId("TEST_SP_ENTITY_ID");
        serviceProvider.setDnsName("TEST_SP_DNS_NAME");
        serviceProvider.setLevelOfAssurance("TEST_SP_LOS");
        serviceProvider.setAttributeLevelOfAssurance("TEST_SP_ALOS");
        serviceProvider.setAcsAddress("TEST_SP_ACS");
        serviceProvider.setProviderType(ProviderType.SERVICE_PROVIDER);
        serviceProvider.setSessionProfile(SessionProfile.TUNNISTUSFI_LEGACY);
        serviceProvider.setVtjVerificationRequired(true);
        List<MetadataDTO> metadataDTOs = new ArrayList<>();
        metadataDTOs.add(serviceProvider);
        doReturn(metadataDTOs).when(metadataClientSpy).getMetadataDTOs(any(CloseableHttpClient.class), any(HttpGet.class));
        // actual test
        Map<String, ServiceProvider> serviceProviders = metadataClientSpy.getServiceProviders();
        assertEquals(1, serviceProviders.size());
        ServiceProvider returnedServiceProvider = serviceProviders.get("TEST_SP_ENTITY_ID");
        assertEquals("TEST_SP_LOS", returnedServiceProvider.getLevelOfAssurance());
        assertEquals("TEST_SP_ALOS", returnedServiceProvider.getPermittedAuthMethods());
        assertEquals(SessionProfile.TUNNISTUSFI_LEGACY, returnedServiceProvider.getSessionProfile());
        assertTrue(returnedServiceProvider.isVtjVerificationRequired());
    }

    @Test
    public void getAuthenticationProviders() throws Exception {
        MetadataClient metadataClientSpy = Mockito.spy(metadataClient);
        MetadataDTO authProvider = new MetadataDTO();
        authProvider.setName("TEST_AUTH_PROVIDER");
        authProvider.setDnsName("TEST_AUTH_PROVIDER_DNS_NAME");
        authProvider.setLevelOfAssurance("fLoA2");
        authProvider.setAttributeLevelOfAssurance("TUPAS");
        authProvider.setAcsAddress("TEST_AUTH_PROVIDER_CONTEXT_URL");
        authProvider.setEntityId("TEST_AUTH_PROVIDER_DB_ENTITY_ID");
        List<MetadataDTO> metadataDTOs = new ArrayList<>();
        metadataDTOs.add(authProvider);
        doReturn(metadataDTOs).when(metadataClientSpy).getMetadataDTOs(any(CloseableHttpClient.class), any(HttpGet.class));

        List<AuthenticationProvider> authProviders = metadataClientSpy.getAuthenticationProviders();
        assertEquals(1, authProviders.size());
        AuthenticationProvider returnedAuthProvider = authProviders.get(0);
        assertEquals("TEST_AUTH_PROVIDER", returnedAuthProvider.getName());
        assertEquals("TEST_AUTH_PROVIDER_DNS_NAME", returnedAuthProvider.getDomainName());
        assertEquals(AuthMethod.fLoA2, returnedAuthProvider.getAuthenticationMethod());
        assertEquals("TEST_AUTH_PROVIDER_CONTEXT_URL", returnedAuthProvider.getAuthProviderAuthContextUrl());
        assertEquals("TEST_AUTH_PROVIDER_DB_ENTITY_ID", returnedAuthProvider.getDbEntityIdAuthContextUrl());
    }
}
