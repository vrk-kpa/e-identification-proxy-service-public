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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fi.vm.kapa.identification.dto.MetadataDTO;
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.ProviderType;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MetadataClient {
    private static final Logger logger = LoggerFactory.getLogger(MetadataClient.class);

    @Value("${metadata.server.url}")
    private String metadataServerUrl;

    public Map<String, ServiceProvider> getServiceProviders() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        final String serviceProviderMetadataReqUrl = metadataServerUrl + "?type=" + ProviderType.SERVICE_PROVIDER.toString();
        logger.debug("url to metadata server: {}", serviceProviderMetadataReqUrl);
        HttpGet getMethod = new HttpGet(serviceProviderMetadataReqUrl);
        final Map<String, ServiceProvider> serviceProviders = new HashMap<>();
        List<MetadataDTO> serviceProvidersAsMetadataDTOs = getMetadataDTOs(httpClient, getMethod);
        if (!CollectionUtils.isEmpty(serviceProvidersAsMetadataDTOs)) {
            serviceProvidersAsMetadataDTOs.forEach(data -> {
                logger.debug("data.getDbEntityIdAuthContextUrlByAuthProviderAuthContextUrl(): " + data.getEntityId());
                logger.debug("--adding serviceProvider - ent ID: " + data.getEntityId() +
                        ", dns: " + data.getDnsName() + ", permitted auth methods: " + data.getAttributeLevelOfAssurance() +
                        ", type: " + data.getProviderType() + ", profile: " + data.getSessionProfile());
                ServiceProvider serviceProvider = new ServiceProvider(data.getEntityId(),
                        data.getLevelOfAssurance(),
                        data.getAttributeLevelOfAssurance(),
                        data.getSessionProfile(),
                        data.isVtjVerificationRequired());
                serviceProviders.put(data.getEntityId(), serviceProvider);
            });
        }
        return serviceProviders;
    }

    public List<AuthenticationProvider> getAuthenticationProviders() {
        List<AuthenticationProvider> providers = new ArrayList<>();
        final String authenticationProviderMetadataReqUrl = metadataServerUrl + "?type=" + ProviderType.AUTHENTICATION_PROVIDER.toString();
        logger.debug("url to metadata server - authenticationProviders: {}", authenticationProviderMetadataReqUrl);
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            logger.debug("url to metadata server: {}", authenticationProviderMetadataReqUrl);
            HttpGet getMethod = new HttpGet(authenticationProviderMetadataReqUrl);
            List<MetadataDTO> metadata = getMetadataDTOs(httpClient, getMethod);
            metadata.forEach(data -> providers.add(new AuthenticationProvider(data.getName() + "",
                    data.getDnsName(),
                    AuthMethod.valueOf(data.getAttributeLevelOfAssurance()),
                    data.getAcsAddress(),
                    data.getEntityId()))
            );
        } catch (Exception e) {
            logger.error("Error updating proxy ApprovedAuthenticationProviders", e);
        }
        return providers;
    }

    List<MetadataDTO> getMetadataDTOs(CloseableHttpClient httpClient, HttpGet getMethod) throws IOException {
        List<MetadataDTO> serviceProviders = new ArrayList<>();
        CloseableHttpResponse response = httpClient.execute(getMethod);

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            Gson gson = new Gson();
            serviceProviders = gson.fromJson(EntityUtils.toString(response.getEntity()),
                    new TypeToken<List<MetadataDTO>>() {
                    }.getType());
            response.close();
        } else {
            logger.warn("Metadata server responded with HTTP {}", statusCode);
            response.close();
        }
        return serviceProviders;
    }

}
