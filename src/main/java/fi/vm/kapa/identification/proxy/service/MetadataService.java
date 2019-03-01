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

import fi.vm.kapa.identification.proxy.exception.AuthenticationProviderNotFoundException;
import fi.vm.kapa.identification.proxy.exception.CountryNotFoundException;
import fi.vm.kapa.identification.proxy.exception.RelyingPartyNotFoundException;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.Country;
import fi.vm.kapa.identification.proxy.metadata.MetadataClient;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import java.util.*;

@Service
@Singleton
public class MetadataService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);

    private MetadataClient metadataClient;

    //cached service providers
    private Map<String,ServiceProvider> serviceProviderMetaDataCache = new HashMap<>();
    //cached authentication providers
    private ApprovedAuthenticationProviders approvedAuthenticationProviders =
            new ApprovedAuthenticationProviders(Collections.emptyList());
    //cached country data
    private Map<String, Country> countryCache = new HashMap<>();

    @SuppressWarnings("unused")
    private MetadataService() {
    }

    @Autowired
    public MetadataService(MetadataClient metadataClient) {
        this.metadataClient = metadataClient;
    }

    public Map<String,ServiceProvider> getServiceProviderMetaDataCache() {
        return serviceProviderMetaDataCache;
    }

    public void setServiceProviderMetaDataCache(Map<String,ServiceProvider> serviceProviderMetaDataCache) {
        logger.debug("Clearing previous serviceProvider cache, content size {}", this.serviceProviderMetaDataCache.size());
        this.serviceProviderMetaDataCache = serviceProviderMetaDataCache;
    }

    public ApprovedAuthenticationProviders getApprovedAuthenticationProviders() {
        return approvedAuthenticationProviders;
    }

    public void setApprovedAuthenticationProviders(ApprovedAuthenticationProviders approvedAuthenticationProviders) {
        this.approvedAuthenticationProviders = approvedAuthenticationProviders;
    }

    public Map<String, Country> getCountryCache() {
        return countryCache;
    }

    public void setCountryCache(Map<String, Country> countryCache) {
        logger.debug("Clearing previous country cache, content size {}", this.countryCache.size());
        this.countryCache = countryCache;
    }

    public ServiceProvider getRelyingParty(String relyingPartyEntityId) throws RelyingPartyNotFoundException {
        ServiceProvider serviceProvider = serviceProviderMetaDataCache.get(relyingPartyEntityId);
        if (null == serviceProvider) {
            throw new RelyingPartyNotFoundException("relyingParty not found: " + relyingPartyEntityId);
        }
        return serviceProvider;
    }

    public AuthenticationProvider getAuthenticationProvider(String authContextUrl) throws AuthenticationProviderNotFoundException {
        AuthenticationProvider authenticationProvider = approvedAuthenticationProviders.getAuthenticationProviderByAuthContextUrl(authContextUrl);
        if (authenticationProvider == null) {
            throw new AuthenticationProviderNotFoundException("authentication provider not found: " + authContextUrl);
        }
        return authenticationProvider;
    }

    public AuthenticationProvider getAuthenticationProviderByEntityId(String entityId) throws AuthenticationProviderNotFoundException {
        AuthenticationProvider authenticationProvider = approvedAuthenticationProviders.getAuthenticationProviderByEntityId(entityId);
        if (authenticationProvider == null) {
            throw new AuthenticationProviderNotFoundException("authentication provider not found by entityId: " + entityId);
        }
        return authenticationProvider;
    }

    public Country getCountry(String countryCode) throws CountryNotFoundException {
        Country country = countryCache.get(countryCode);
        if (null == country) {
            throw new CountryNotFoundException("Country not found: " + countryCode);
        }
        return country;
    }

    public void updateMetadataCache() {
        try {
            MetadataService.ApprovedAuthenticationProviders newAuthenticationProviders =
                    getAuthenticationProviders();
            Map<String,ServiceProvider> newServiceProviders = metadataClient.getServiceProviders();

            if (!newServiceProviders.isEmpty() && !newAuthenticationProviders.allProviders.isEmpty()) {
                setServiceProviderMetaDataCache(newServiceProviders); // when done replace old with new
                setApprovedAuthenticationProviders(newAuthenticationProviders);
            }

            Map<String, Country> newCountries = metadataClient.getCountries();
            if ( !newCountries.isEmpty() ) {
                setCountryCache(newCountries); // when done replace old with new
            }
        } catch (Exception e) {
            logger.error("Error updating proxy metadata", e);
        }
    }

    public ApprovedAuthenticationProviders getAuthenticationProviders() {
        List<AuthenticationProvider> providerDTOs = metadataClient.getAuthenticationProviders();
        return new MetadataService.ApprovedAuthenticationProviders(providerDTOs);
    }

    public static class ApprovedAuthenticationProviders {

        private final List<AuthenticationProvider> allProviders;

        public ApprovedAuthenticationProviders(List<AuthenticationProvider> providers) {
            allProviders = providers;
        }

        private AuthenticationProvider getAuthenticationProviderByAuthContextUrl(String authContextUrl) {
            for (AuthenticationProvider authProvider : allProviders) {
                if (authProvider.getAuthProviderAuthContextUrl().equals(authContextUrl)) {
                    return authProvider;
                }
            }
            return null;
        }

        private AuthenticationProvider getAuthenticationProviderByEntityId(String entityId) {
            for (AuthenticationProvider authProvider : allProviders) {
                if (authProvider.getDbEntityIdAuthContextUrl().equals(entityId)) {
                    return authProvider;
                }
            }
            return null;
        }

        public List<AuthenticationProvider> getAllProviders() {
            return allProviders;
        }
    }
}
