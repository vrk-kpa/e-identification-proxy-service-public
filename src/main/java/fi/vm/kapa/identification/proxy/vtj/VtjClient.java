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
package fi.vm.kapa.identification.proxy.vtj;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.kapa.identification.proxy.session.Identity;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.vtj.model.VTJResponse;


@Component
public class VtjClient {

    @Value("${vtj.client.url}")
    private String vtjClientEndpoint;
    
    private static final Logger logger = LoggerFactory.getLogger(VtjClient.class);

    public VTJResponse fetchVtjData(Identity identity) throws VtjServiceException {
        VTJResponse vtjResponse = getVtjResponseForUser(identity);
        if (vtjResponse == null) {
           logger.debug("VTJ returned no data for user " + identity.getIdentifier());
        }
        return vtjResponse;
    }

    VTJResponse getVtjResponseForUser(Identity identity) throws VtjServiceException {
        Response response = getVtjHttpResponse(identity);
        if (response.getStatus() == HttpStatus.OK.value()) {
            return response.readEntity(VTJResponse.class);
        } else {
            logger.error("Vtj connection error: " + response.getStatus());
            throw new VtjServiceException("Vtj connection error: " + response.getStatus());
        }
    }

    Response getVtjHttpResponse(Identity identity) throws VtjServiceException {
        Response response;
        try {
            WebTarget webTarget = getClient()
                    .target(vtjClientEndpoint)
                    .path(identity.getIdentifier())
                    .path(identity.getIdentifierType().name())
                    .queryParam("issuerDn", identity.getIssuerDn());

            Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
            response = invocationBuilder.get();
        } catch (Exception e) {
            logger.error("Vtj connection not established. Service request failed.", e);
            throw new VtjServiceException("Vtj connection not established. Service request failed.");
        }
        return response;
    }

    private Client getClient() {
        ClientConfig clientConfig = new ClientConfig();
        Client client = ClientBuilder.newClient(clientConfig);
        client.register(JacksonFeature.class);
        return client;
    }

}
