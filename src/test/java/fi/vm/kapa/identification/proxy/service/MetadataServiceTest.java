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

import fi.vm.kapa.identification.proxy.exception.RelyingPartyNotFoundException;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.MetadataClient;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.SessionProfile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MetadataServiceTest {

    @Mock
    MetadataClient metadataClient;

    @Autowired
    @InjectMocks
    private MetadataService metadataService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getRelyingParty() throws Exception {
        metadataService.getServiceProviderMetaDataCache().put("FOUND", mock(ServiceProvider.class));
        assertNotNull(metadataService.getRelyingParty("FOUND"));
    }

    @Test(expected = RelyingPartyNotFoundException.class)
    public void getRelyingPartyThrowsWhenNotFound() throws Exception {
        metadataService.getRelyingParty("NOT_FOUND");
    }

    @Test(expected = RelyingPartyNotFoundException.class)
    public void getRelyingPartyThrowsWhenEntityIdIsNull() throws Exception {
        metadataService.getRelyingParty(null);
    }

    ServiceProvider getDefaultServiceProvider() {
        return new ServiceProvider("TEST_ENTITY_ID", "TEST_LOA",
                "TEST_ATTRIBUTE_LOA",
                SessionProfile.VETUMA_SAML2, true);
    }

    @Test
    public void updateMetadataCacheInsertsValues() throws Exception {
        MetadataService metadataServiceSpy = spy(this.metadataService);
        MetadataService.ApprovedAuthenticationProviders authenticationProviders =
            getApprovedAuthenticationProvidersWithDefaultNamedProvider("TEST_AUTHENTICATION_PROVIDER");
        doReturn(authenticationProviders).when(metadataServiceSpy).getAuthenticationProviders();
        ServiceProvider serviceProvider = getDefaultServiceProvider();
        Map<String, ServiceProvider> serviceProviders = new HashMap<>();
        serviceProviders.put("entityId", serviceProvider);
        when(metadataClient.getServiceProviders()).thenReturn(serviceProviders);
        // precondition
        assertTrue(metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().isEmpty());
        assertTrue(metadataServiceSpy.getServiceProviderMetaDataCache().isEmpty());
        // actual test
        metadataServiceSpy.updateMetadataCache();
        assertEquals(1, metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataServiceSpy.getServiceProviderMetaDataCache().size());
    }

    @Test
    public void updateMetadataReplacesWithNewServiceProviders() throws Exception {
        initializeMetadataServiceWithMetadata("TEST_AUTHENTICATION_PROVIDER", "TEST_SERVICE_PROVIDER");
        assertEquals("TEST_SERVICE_PROVIDER", metadataService.getServiceProviderMetaDataCache().get("entityId").getEntityId());

        MetadataService metadataServiceSpy = spy(this.metadataService);
        MetadataService.ApprovedAuthenticationProviders authenticationProviders = getApprovedAuthenticationProvidersWithDefaultNamedProvider("TEST_AUTHENTICATION_PROVIDER");
        doReturn(authenticationProviders).when(metadataServiceSpy).getAuthenticationProviders();

        Map<String, ServiceProvider> serviceProviders = getServicesProviderMapWithKeyAndEntityId("entityId", "TEST_SERVICE_PROVIDER_NEW");
        when(metadataClient.getServiceProviders()).thenReturn(serviceProviders);
        // actual test
        metadataServiceSpy.updateMetadataCache();
        assertEquals(1, metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataServiceSpy.getServiceProviderMetaDataCache().size());
        assertEquals("TEST_SERVICE_PROVIDER_NEW", metadataServiceSpy.getServiceProviderMetaDataCache().get("entityId").getEntityId());
        assertEquals("TEST_AUTHENTICATION_PROVIDER",
                metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().get(0).getName());
    }

    @Test
    public void updateMetadataReplacesWithNewAuthenticationProviders() throws Exception {
        initializeMetadataServiceWithMetadata("TEST_AUTHENTICATION_PROVIDER", "TEST_SERVICE_PROVIDER");
        assertEquals("TEST_AUTHENTICATION_PROVIDER",
                metadataService.getApprovedAuthenticationProviders().getAllProviders().get(0).getName());

        MetadataService metadataServiceSpy = spy(this.metadataService);
        MetadataService.ApprovedAuthenticationProviders authenticationProviders = getApprovedAuthenticationProvidersWithDefaultNamedProvider("TEST_AUTHENTICATION_PROVIDER_NEW");
        doReturn(authenticationProviders).when(metadataServiceSpy).getAuthenticationProviders();

        Map<String, ServiceProvider> serviceProviders = getServicesProviderMapWithKeyAndEntityId("entityId", "TEST_SERVICE_PROVIDER");
        when(metadataClient.getServiceProviders()).thenReturn(serviceProviders);
        // actual test
        metadataServiceSpy.updateMetadataCache();
        assertEquals(1, metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataServiceSpy.getServiceProviderMetaDataCache().size());
        assertEquals("TEST_SERVICE_PROVIDER", metadataServiceSpy.getServiceProviderMetaDataCache().get("entityId").getEntityId());
        assertEquals("TEST_AUTHENTICATION_PROVIDER_NEW",
                metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().get(0).getName());
    }

    @Test
    public void updateMetadataCacheDoesNotChangeCacheWhenGetsEmptyAuthenticationProviders() throws Exception {
        initializeMetadataServiceWithMetadata("TEST_AUTHENTICATION_PROVIDER", "TEST_SERVICE_PROVIDER");
        assertEquals(1, metadataService.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataService.getServiceProviderMetaDataCache().size());

        MetadataService metadataServiceSpy = spy(this.metadataService);
        doReturn(new MetadataService.ApprovedAuthenticationProviders(emptyList())).when(metadataServiceSpy).getAuthenticationProviders();
        // actual test
        metadataService.updateMetadataCache();
        assertEquals(1, metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataServiceSpy.getServiceProviderMetaDataCache().size());
    }

    @Test
    public void updateMetadataCacheDoesNotChangeCacheWhenGetsEmptyServiceProviders() throws Exception {
        initializeMetadataServiceWithMetadata("TEST_AUTHENTICATION_PROVIDER", "TEST_SERVICE_PROVIDER");
        assertEquals(1, metadataService.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataService.getServiceProviderMetaDataCache().size());

        MetadataService metadataServiceSpy = spy(this.metadataService);
        when(metadataClient.getServiceProviders()).thenReturn(new HashMap<>());
        // actual test
        metadataService.updateMetadataCache();
        assertEquals(1, metadataServiceSpy.getApprovedAuthenticationProviders().getAllProviders().size());
        assertEquals(1, metadataServiceSpy.getServiceProviderMetaDataCache().size());
    }

    private void initializeMetadataServiceWithMetadata(String authenticationProviderName, String entityId) {
        MetadataService.ApprovedAuthenticationProviders authenticationProviders =
                getApprovedAuthenticationProvidersWithDefaultNamedProvider(authenticationProviderName);
        Map<String, ServiceProvider> serviceProviders = getServicesProviderMapWithKeyAndEntityId("entityId", entityId);
        metadataService.setApprovedAuthenticationProviders(authenticationProviders);
        metadataService.setServiceProviderMetaDataCache(serviceProviders);
    }

    private MetadataService.ApprovedAuthenticationProviders getApprovedAuthenticationProvidersWithDefaultNamedProvider(String name) {
        AuthenticationProvider authenticationProviderDTO = new AuthenticationProvider(name, "TEST_DOMAIN_NAME", AuthMethod.TESTI, "TEST_AUTH_CONTEXT_URL", "TEST_DB_ENTITY_URL");
        List<AuthenticationProvider> authenticationProviderDTOs = Arrays.asList(authenticationProviderDTO);
        return new MetadataService.ApprovedAuthenticationProviders(authenticationProviderDTOs);
    }

    private Map<String, ServiceProvider> getServicesProviderMapWithKeyAndEntityId(String key, String serviceProviderName) {
        ServiceProvider serviceProvider = getServiceProviderWithEntityIdAndVtjVerificationRequired(serviceProviderName, true);
        Map<String, ServiceProvider> serviceProviders = new HashMap<>();
        serviceProviders.put(key, serviceProvider);
        return serviceProviders;
    }

    private ServiceProvider getServiceProviderWithEntityIdAndVtjVerificationRequired(String entityId, boolean vtjVerificationRequired) {
        return new ServiceProvider(entityId, "TEST_LOA", "TEST_ATTRIBUTE_LOA",
                SessionProfile.VETUMA_SAML2, vtjVerificationRequired);
    }

}