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
import fi.vm.kapa.identification.proxy.exception.AttributeGenerationException;
import fi.vm.kapa.identification.proxy.service.SessionHandlingService;
import fi.vm.kapa.identification.resource.ProxyResource;
import fi.vm.kapa.identification.type.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;

@Component
@Path("/proxy")
public class ProxyResourceImpl implements ProxyResource {

    private static final Logger logger = LoggerFactory.getLogger(ProxyResourceImpl.class);

    @Autowired
    private SessionHandlingService sessionHandlingService;

    @Override
    public ProxyMessageDTO fromIdPInitSession(String relyingParty,
                                              String entityId,
                                              String countryCode,
                                              String uid,
                                              String key,
                                              String authMethodReqStr,
                                              String logTag) {
        logger.debug("Got session init from Shibboleth IdP, conversation key: {}", key);

        ProxyMessageDTO message = sessionHandlingService.initNewSession(relyingParty, entityId, countryCode, uid, key, authMethodReqStr, logTag);
        if (message.getErrorType() == ErrorType.NO_ERROR) {
            return message;
        } else {
            throw resolveError(message);
        }
    }

    @Override
    public ProxyMessageDTO fromIdPRequestSession(String tokenId, String phaseId, String logTag) {
        logger.debug("Got session request from Shibboleth IdP, tokenId: {}, phase ID: {}", tokenId, phaseId);

        ProxyMessageDTO message = sessionHandlingService.getSessionById(tokenId, phaseId, logTag);
        if (message.getErrorType() == ErrorType.NO_ERROR) {
            return message;
        } else {
            throw resolveError(message);
        }
    }

    @Override
    public ProxyMessageDTO fromIdPPurgeSession(String tokenId, String phaseId, String logTag) {
        logger.debug("Got purge request from Shibboleth IdP, tokenId: {}, phase ID: {}", tokenId, phaseId);

        ProxyMessageDTO message = sessionHandlingService.removeSessionById(tokenId, phaseId, logTag);
        if (message.getErrorType() == ErrorType.NO_ERROR) {
            return message;
        } else {
            throw resolveError(message);
        }
    }

    @Override
    public SessionAttributeDTO getSessionAttributes(String uid, String authMethodOid, String relyingParty, boolean tokenRequired, String authnRequestId) {
        logger.debug("Got session attributes request from Shibboleth IdP, uid: {}, authMethodOid: {}, relyingParty: {}, tokenRequired: {}, authnRequestId: {}", uid, authMethodOid, relyingParty, tokenRequired, authnRequestId);

        SessionAttributeDTO attributes = null;
        try {
            attributes = sessionHandlingService.getSessionAttributes(uid, authMethodOid, relyingParty, tokenRequired, authnRequestId);
        } catch (AttributeGenerationException e) {
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (!CollectionUtils.isEmpty(attributes.getAttributeMap())) {
            return attributes;
        } else {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
    }

    @Override
    public ProxyMessageDTO fromSPBuildSessionPost(String tokenId, String phaseId,
                                                  String logTag, Map<String,String> spSessionData) {
        logger.debug("Got build session POST request from Shibboleth SP, tokenId: {} , phaseId: {}", tokenId, phaseId);

        ProxyMessageDTO message = sessionHandlingService.buildNewSession(tokenId, phaseId, spSessionData, logTag);
        if (message.getErrorType() == ErrorType.NO_ERROR) {
            return message;
        } else {
            throw resolveError(message);
        }
    }

    private WebApplicationException resolveError(ProxyMessageDTO message) {
        if (message.getErrorType() == ErrorType.VTJ_FAILED) {
            return new ServiceUnavailableException();
        } else if (message.getErrorType() == ErrorType.INTERNAL_ERROR) {
            return new InternalServerErrorException();
        } else {
            return new BadRequestException();
        }
    }
}
