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
import fi.vm.kapa.identification.proxy.exception.*;
import fi.vm.kapa.identification.proxy.person.IdentifiedPersonBuilder;
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.*;
import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.metadata.ServiceProvider;
import fi.vm.kapa.identification.proxy.utils.SessionHandlingUtils;
import fi.vm.kapa.identification.service.PhaseIdHistoryService;
import fi.vm.kapa.identification.service.PhaseIdService;
import fi.vm.kapa.identification.type.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import java.util.Map;

import static fi.vm.kapa.identification.proxy.session.VtjVerificationRequirement.FORBIDDEN;
import static fi.vm.kapa.identification.proxy.session.VtjVerificationRequirement.MAY_FAIL;
import static fi.vm.kapa.identification.proxy.session.VtjVerificationRequirement.MUST_SUCCEED;

@Service
public class SessionHandlingService {

    private static final Logger logger = LoggerFactory.getLogger(SessionHandlingService.class);

    private PhaseIdService phaseIdInitSession;
    private PhaseIdService phaseIdBuiltSession;
    private PhaseIdHistoryService historyService;
    private IdentifiedPersonBuilder sessionPersonBuilder;
    private SessionAttributeCollector sessionAttributeCollector;

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

    @Value("${saml.vtj.required.key}")
    private String vtjRequiredKey;

    @Value("${saml.vtj.verified.key}")
    private String vtjVerifiedKey;

    @Value("${saml.vtj.invalid.key}")
    private String vtjDataInvalidKey;

    @Value("${saml.national.id.key}")
    private String samlNationalIdKey;

    @Value("${legacy.version.key}")
    private String legacyVersionKey;

    @Value("${legacy.version.value}")
    private String legacyVersionValue;

    private VtjPersonService vtjPersonService;

    private SessionHandlingUtils sessionHandlingUtils;

    private MetadataService metadataService;

    private UidToUserSessionsCache uidToUserSessionsCache;

    @Autowired
    public SessionHandlingService(MetadataService metadataService,
                                  VtjPersonService vtjPersonService,
                                  SessionHandlingUtils sessionHandlingUtils,
                                  @Named("personBuilder")
                                  IdentifiedPersonBuilder sessionPersonBuilder,
                                  SessionAttributeCollector sessionAttributeCollector,
                                  UidToUserSessionsCache uidToUserSessionsCache,
                                  @Named("sessionInitPhaseIdService")
                                          PhaseIdService phaseIdInitSession,
                                  @Named("sessionBuiltPhaseIdService")
                                          PhaseIdService phaseIdBuiltSession) {
        this.metadataService = metadataService;
        this.vtjPersonService = vtjPersonService;
        this.sessionHandlingUtils = sessionHandlingUtils;
        this.sessionPersonBuilder = sessionPersonBuilder;
        this.sessionAttributeCollector = sessionAttributeCollector;
        this.uidToUserSessionsCache = uidToUserSessionsCache;
        this.phaseIdInitSession = phaseIdInitSession;
        this.phaseIdBuiltSession = phaseIdBuiltSession;
        this.historyService = PhaseIdHistoryService.getInstance();
    }

    @PostConstruct
    public void initSessionHandlingService() {
        try {
            // This must be done in order to guarantee that Proxy is immediately able to work properly
            metadataService.updateMetadataCache();
        } catch (Exception e) {
            logger.error("Error initializing session handler", e);
        }
    }

    /**
     * Initializes new session with basic session structure and
     * stores the IdP's given conversation key into the session data.
     *
     * @param relyingPartyId    the original service provider
     * @param uid             existing IdP session uid
     * @param conversationKey key generated by IdP
     * @param requestedAuthenticationMethodStr auth methods requested by SP
     * @param logTag          identifier used to pin point errors from logs
     * @return message DTO class used to transfer data from Proxy
     */
    public ProxyMessageDTO initNewSession(String relyingPartyId, String uid, String conversationKey, String requestedAuthenticationMethodStr, String logTag) {

        logger.debug("initNewSession called with relyingPartyId: {}, uid: {}, conversationKey {}, requestedAuthenticationMethodStr: {}, logTag: {}", relyingPartyId, uid, conversationKey, requestedAuthenticationMethodStr, logTag);

        ProxyMessageDTO message = new ProxyMessageDTO();

        try {
            ServiceProvider relyingParty = metadataService.getRelyingParty(relyingPartyId);
            String tokenId = phaseIdInitSession.nextTokenId();
            String phaseId = phaseIdInitSession.newPhaseId(tokenId, stepSessionInit);

            if (StringUtils.isNotBlank(tokenId) && StringUtils.isNotBlank(phaseId) &&
                    relyingParty.getSessionProfile() != null) {

                Session session = new Session();
                /* ------------------------------------------------------------------
                 * NOTE! The session ID requires further specification on how
                 * to define the session ID which can be used across multiple proxies
                 * ----------------------------------------------------------------- */
                //uid is either existing session uid from IdP or SessionStatus.INIT ("0")
                session.setUid(uid);
                logger.debug("Set proxy init session uid to: " + uid);
                session.setSessionId("not_specified_yet");
                session.setConversationKey(conversationKey);
                session.setSessionProfile(relyingParty.getSessionProfile());
                session.setRelyingPartyEntityId(relyingPartyId);

                String permittedAuthMethodsStr = relyingParty.getPermittedAuthMethods();
                //Permitted authentication methods for SP cannot be empty
                if (StringUtils.isBlank(permittedAuthMethodsStr)) {
                    throw new Exception("Error in requesting explicit authentication methods");
                } else if (StringUtils.isBlank(requestedAuthenticationMethodStr)) {
                    //Set metadata defaults if requested is empty
                    logger.debug("Requested authentication methods was empty, setting relying party defaults");
                    session.setRequestedAuthenticationMethods(permittedAuthMethodsStr);
                } else if (relyingParty.isAuthMethodListPermitted(requestedAuthenticationMethodStr)) {
                    session.setRequestedAuthenticationMethods(requestedAuthenticationMethodStr);
                } else {
                    logger.warn("Requested authentication methods not permitted within event:" + logTag);
                    throw new Exception("Requested authentication methods not permitted within event");
                }
                session.setTimestamp();
                //AuthMethod is INIT at this point
                uidToUserSessionsCache.insertIntoSessionCache(tokenId, AuthMethod.INIT, session);

                message.setAuthenticationMethods(session.getRequestedAuthenticationMethods());
                message.setTokenId(tokenId);
                message.setPhaseId(phaseId);
                message.setErrorType(ErrorType.NO_ERROR);
                message.setLoaProfile(relyingParty.getLevelOfAssurance());

                logger.debug("--session initialized, token ID: {}, phase ID: {}", message.getTokenId(), message.getPhaseId());
            } else {
                logger.warn("<<" + logTag + ">> Initializing session failed, token ID, phase ID or session profile missing " +
                        "- tid: " + tokenId + ", pid: " + phaseId + ", session profile: " + relyingParty.getSessionProfile());

                message.setErrorType(ErrorType.SESSION_INIT_FAILED);
            }
        } catch (RelyingPartyNotFoundException e) {
            logger.warn("<<" + logTag + ">> Initializing session failed. ", e);
            message.setErrorType(ErrorType.SESSION_INIT_FAILED);
        }
        catch (Exception e) {
            logger.error("<<{}>> Error initializing new session", logTag, e);
            message.setErrorType(ErrorType.INTERNAL_ERROR);
        }

        return message;
    }

    /**
     * Builds the identity session with the data received from SP and from X-Road. This method uses
     * a deduction logic from SP's session data, identifier and its type, taking into account
     * the original service provider plus additional restrictions based upon those to build the
     * actual identity session data
     *
     * @param tokenId       token ID used to verify the sanity of the request
     * @param phaseId       the checksum value that should be generated from the tokenId
     * @param spSessionData raw data received from SP
     * @param logTag        identifier used to pin point errors from logs
     * @return message DTO class used to transfer data from Proxy
     */
    public ProxyMessageDTO buildNewSession(String tokenId, String phaseId, Map<String, String> spSessionData, String logTag) {
        ProxyMessageDTO message = new ProxyMessageDTO();

        // NOTE! This is only for development purposes, this will be removed
        logger.debug("SP build session data:");
        spSessionData.keySet().forEach(key -> logger.debug("--" + key + " <--> " + spSessionData.get(key)));
        try {

            if (historyService.areIdsConsumed(tokenId, phaseId)) {
                logger.warn("Received already consumed token and phase IDs!!");
                message.setErrorType(ErrorType.SESSION_BUILD_FAILED);
            } else if (phaseIdInitSession.verifyPhaseId(phaseId, tokenId, stepSessionBuild)) {

                Session session = uidToUserSessionsCache.getSessionByKeyAndAuthMethod(tokenId, AuthMethod.INIT);
                logger.debug("Session with token ID " + tokenId + " exists: " + (session != null ? "YES" : "NO"));
                //Check which authentication context (method) is in use
                String spProvidedEndIdPAuthContextUrl = sessionHandlingUtils.getSpProvidedEndIdPAuthContextUrl(spSessionData);

                AuthenticationProvider authenticationProvider = metadataService.getAuthenticationProvider(spProvidedEndIdPAuthContextUrl);

                if (session != null && !AuthMethodHelper.authMethodInPermittedMethods(authenticationProvider.getAuthenticationMethod().name(),
                        session.getRequestedAuthenticationMethods())) {
                    logger.error("Error building new session: used authentication method not permitted ");
                    throw new Exception("Error building new session: used authentication method not permitted ");
                }

                IdentifiedPerson identifiedPerson = sessionPersonBuilder.build(spSessionData, authenticationProvider.getAuthenticationMethod());
                if (session != null && identifiedPerson.getIdentity().getIdentifierType() != null) {

                     /* Only those values what the IdP can understand, can be used from the SP's session
                     * data package since different true identity providers can supply different
                     * data and only those which are required for the identity session
                     * can be used, note that legacy data must be also added in certain cases
                     */
                    session.setIdentifiedPerson(identifiedPerson);
                    session.setAuthenticationProvider(authenticationProvider);
                    if (session.getSessionProfile() == SessionProfile.TUNNISTUSFI_LEGACY) {
                        session.setLegacyVersion(sessionHandlingUtils.getLegacyVersion(spSessionData));
                    }

                    /* The session data is enriched with additional data fetched from X-Road suppliers which map
                     * given electronic ID or SSN to basic information such as names, addresses etc.
                     */
                    VtjVerificationRequirement vtjVerification = getVtjVerificationRequirement(metadataService.getRelyingParty(session.getRelyingPartyEntityId()),
                            authenticationProvider.getAuthenticationMethod());
                    session.setVtjVerificationRequired(vtjVerification == MUST_SUCCEED);

                    /* VTJ data retrieval validates the person at hand and may throw
                     * InvalidVtjDataException or VtjServiceException.
                     * InvalidVtjDataException invalidates all existing session entries and is
                     * passed on (authentication flow will fail).
                     * VtjServiceException is ignored if vtjVerificationRequired is false (continue
                     * with null vtjData).
                     */
                    if (vtjVerification != FORBIDDEN) {
                        try {
                            VtjPerson vtjPerson = vtjPersonService.getVtjPerson(identifiedPerson);
                            vtjPerson.validate();
                            session.setVtjPerson(vtjPerson);
                            session.setVtjVerified(true);
                        } catch (InvalidVtjDataException e) {
                            if (!session.getUid().contentEquals(SessionStatus.INIT.getStatusAsNumericalString())) {
                                uidToUserSessionsCache.invalidateCachedSessionsByKey(session.getUid());
                            }
                            throw e;
                        } catch (VtjServiceException e) {
                            if (vtjVerification == MUST_SUCCEED) {
                                throw e;
                            }
                        }
                    }

                    /* This uid value is a session reference between Proxy sessions and the IdP session.
                     * If uid already has a value other than SessionStatus.INIT,
                     * a session already exists in IdP and it is conserved.
                     * Otherwise assign new value.
                     */
                    if (session.getUid().contentEquals(SessionStatus.INIT.getStatusAsNumericalString())) {
                        String uid = phaseIdBuiltSession.nextTokenId();
                        //Collision check for extra safety
                        while (uidToUserSessionsCache.cacheContainsKey(uid)) {
                            uid = phaseIdBuiltSession.nextTokenId();
                        }
                        session.setUid(uid);
                        logger.debug("Generated UID: {}", uid);
                    }

                    /* If invalid session entries exist, cancel authentication
                     */
                    if (uidToUserSessionsCache.invalidSessionsInCacheByKey(session.getUid())) {
                        throw new InvalidVtjDataException("Invalid sessions in session cache, cannot authenticate");
                    }

                    session.setValidated(true);
                    session.setTimestamp();
                    /* Initial session token must be removed since new token is used to store the
                     * actual session data, this is based on security since the initial token is
                     * exposed to external sources in HTTP 302 requests
                     */

                    String nextTokenId = phaseIdBuiltSession.nextTokenId();
                    logger.debug("Update session cache with tokenId: {}, nextTokenId: {}, authProviderAuthMethod: {}", tokenId, nextTokenId, authenticationProvider.getAuthenticationMethod());
                    uidToUserSessionsCache.replaceSessionCacheKey(tokenId, nextTokenId, authenticationProvider.getAuthenticationMethod(), session);
                    if (logger.isDebugEnabled()) {
                        uidToUserSessionsCache.debugLogSessionStatus();
                    }

                    String nextPhaseId = phaseIdBuiltSession.newPhaseId(nextTokenId, stepRedirectFromSP);
                    message.setTokenId(nextTokenId);
                    message.setPhaseId(nextPhaseId);
                    message.setErrorType(ErrorType.NO_ERROR);
                } else {
                    logger.warn("<<" + logTag + ">> Building session failed, identifier type missing - tid: "
                            + tokenId + ", pid: " + phaseId + ", identifierType: " + identifiedPerson.getIdentity().getIdentifierType());
                    message.setErrorType(ErrorType.SESSION_BUILD_FAILED);
                }
            } else {
                logger.warn("<<" + logTag + ">> Building session failed, token ID, phase ID - tid: "
                        + tokenId + ", pid: " + phaseId);
                message.setErrorType(ErrorType.SESSION_BUILD_FAILED);
            }
        } catch (AuthenticationProviderNotFoundException e) {
            logger.error("<<{}>> Error building new session", logTag, e);
            message.setErrorType(ErrorType.INTERNAL_ERROR);
        } catch (InvalidVtjDataException ve) {
            logger.error("<<{}>> Error building new session", logTag, ve);
            message.setErrorType(ErrorType.VTJ_INVALID);
        } catch (VtjServiceException se) {
            logger.error("<<{}>> Error building new session", logTag, se);
            message.setErrorType(ErrorType.VTJ_FAILED);
        } catch (Exception e) {
            logger.error("<<{}>> Error building new session", logTag, e);
            message.setErrorType(ErrorType.INTERNAL_ERROR);
        }

        return message;
    }

    /**
     * Returns session's basic information as a DTO class. Note that this method
     * doesn't return the attributes, just the basic identifier and conversation key
     *
     * @param tokenId token ID used to verify the sanity of the request
     * @param phaseId the checksum value that should be generated from the tokenId
     * @param logTag  identifier used to pin point errors from logs
     * @return message DTO class used to transfer data from Proxy
     */
    public ProxyMessageDTO getSessionById(String tokenId, String phaseId, String logTag) {
        ProxyMessageDTO message = new ProxyMessageDTO();

        try {
            if (phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepGetSession)) {
                Map<AuthMethod,Session> sessionDTOMap = uidToUserSessionsCache.getSessionDTOMapByKey(tokenId);
                //Check that init-phase map only has one value
                if (sessionDTOMap.size() != 1) {
                    logger.warn("<<{}>> Initial session DTO map size invalid", logTag);
                    message.setErrorType(ErrorType.SESSION_FINALISE_FAILED);
                } else {
                    AuthMethod authMethod = sessionDTOMap.entrySet().iterator().next().getKey();
                    Session session = sessionDTOMap.get(authMethod);

                    if (session != null && session.isValidated()) {
                        message.setConversationKey(session.getConversationKey());
                        message.setUid(session.getUid());
                        message.setUsedAuthenticationMethod(authMethod.toString());
                    /* Since the token values are exposed through browser
                     * in HTTP 302 requests, the session data with old token
                     * is removed and re-inserted with UID value, this UID can
                     * be used to fetch the session data later on by other SPs
                     */
                        uidToUserSessionsCache.replaceSessionCacheKey(tokenId, session.getUid(), authMethod, session);
                        message.setErrorType(ErrorType.NO_ERROR);

                        if (logger.isDebugEnabled()) {
                            uidToUserSessionsCache.debugLogSessionStatus();
                        }
                    }
                }
            } else {
                logger.warn("<<{}>> Phase ID is invalid", logTag);
                message.setErrorType(ErrorType.PHASE_ID_FAILED);
            }
        } catch (Exception e) {
            logger.error("<<{}>> Error in verifying phase ID", logTag, e);

            message.setErrorType(ErrorType.INTERNAL_ERROR);
        }

        return message;
    }

    /**
     * Returns a DTO class containing the bulk session data.
     * The relying party (SP) asking for the attributes is checked against
     * the requested authentication method.
     *
     * This method must also trigger a VTJ-query if it has previously failed,
     * and also set the appropriate session variables.
     *
     * @param uid unique person identifier
     * @param authMethodOid authentication method oid value
     * @param relyingPartyId SP entity ID
     * @return attribute DTO class containing the session attributes
     */
    public SessionAttributeDTO getSessionAttributes(String uid, String authMethodOid, String relyingPartyId) throws AttributeGenerationException {
        SessionAttributeDTO attributes = new SessionAttributeDTO();
        Session session = null;
        AuthMethod requestedAuthMethod = resolveAuthMethodFromOid(authMethodOid);
        if (requestedAuthMethod == null) {
            logger.warn("Requested AuthMethod not found: uid {}, authMethodOid: {}, relyingPartyId: {}", uid, authMethodOid, relyingPartyId);
            return null;
        }
        ServiceProvider relyingParty = null;
        //Check that requested method is permitted for relying party (SP entityID)
        try {
            relyingParty = metadataService.getRelyingParty(relyingPartyId);
            String permittedAuthMethods = relyingParty.getPermittedAuthMethods();
            if (StringUtils.isNotBlank(permittedAuthMethods)
                    && relyingParty.isAuthMethodListPermitted(requestedAuthMethod.name())) {
                session = uidToUserSessionsCache.getSessionByKeyAndAuthMethod(uid, requestedAuthMethod);
                logger.debug("Session attributes with uid: {}, authMethodOid: {}, authMethod: {} returned", uid, authMethodOid, requestedAuthMethod.toString());
            } else {
                logger.warn("Session attributes lookup failed because of invalid requested auth method: {} for relying party: {}", authMethodOid, relyingPartyId);
            }
        } catch (RelyingPartyNotFoundException e) {
            logger.warn("Session attribute lookup with uid: {}, authMethodOid: {}, relyingPartyId: {} failed", uid, authMethodOid, relyingPartyId, e);
            return null;
        }
        if (session == null) {
            logger.warn("Session attribute lookup with uid: {}, authMethodOid: {}, relyingPartyId: {} failed", uid, authMethodOid, relyingPartyId);
        } else {
            //Update session relying party to match the current, latest request
            session.setRelyingPartyEntityId(relyingPartyId);

            //If session data is not VTJ verified, proceed with a VTJ re-query and update session cache
            if (!session.isVtjVerified()) {
                logger.debug("Session not VTJ verified, attempting VTJ re-query");
                try {
                    VtjVerificationRequirement vtjVerification = getVtjVerificationRequirement(relyingParty, requestedAuthMethod);
                    session.setVtjVerificationRequired(vtjVerification == MUST_SUCCEED);

                    /* VTJ data retrieval validates the person at hand and may throw
                     * InvalidVtjDataException or VtjServiceException.
                     * InvalidVtjDataException invalidates the session.
                     * VtjServiceException is ignored if vtjVerificationRequired is false (continue
                     * with null vtjData).
                     */
                    if (vtjVerification != FORBIDDEN) {
                        try {
                            VtjPerson vtjPerson = vtjPersonService.getVtjPerson(session.getIdentifiedPerson());
                            vtjPerson.validate();
                            session.setVtjPerson(vtjPerson);
                            session.setVtjVerified(true);
                        } catch (InvalidVtjDataException e) {
                            session.setVtjDataInvalid(true);
                            uidToUserSessionsCache.invalidateCachedSessionsByKey(uid);
                        } catch (VtjServiceException e) {
                            logger.warn("VTJ re-query failed to complete. Updating session accordingly");
                        }
                    }

                    /* If invalid session entries exist, invalidate all of them.
                     * This session is included in the check.
                     */
                    if (uidToUserSessionsCache.invalidSessionsInCacheByKey(uid)) {
                        logger.error("Session includes invalidated entries, invalidating all entries");
                        session.setVtjDataInvalid(true);
                        uidToUserSessionsCache.invalidateCachedSessionsByKey(uid);
                    }

                    logger.debug("Update session cache with uid: {}, authProviderAuthMethod: {}", uid, requestedAuthMethod);
                    uidToUserSessionsCache.insertIntoSessionCache(uid, requestedAuthMethod, session);

                    if (logger.isDebugEnabled()) {
                        uidToUserSessionsCache.debugLogSessionStatus();
                    }
                } catch (Exception e) {
                    logger.error("Error in session attribute VTJ re-query");
                    return null;
                }
            } else {
                logger.debug("samlVtjVerified true");
            }

            /* Add VTJ attributes to session data for use by IdP
             */
            attributes.getAttributeMap().putAll(sessionAttributeCollector.getAttributes(session));

        }

        return attributes;
    }

    AuthMethod resolveAuthMethodFromOid(String authMethodOid) {
        try {
            AuthMethod authMethod = AuthMethod.fromOid(authMethodOid);
            if (authMethod != AuthMethod.INIT) {
                return authMethod;
            } else {
                return null;
            }
        } catch (AuthMethod.IllegalOidException e) {
            return null;
        }
    }

    VtjVerificationRequirement getVtjVerificationRequirement(ServiceProvider relyingParty, AuthMethod requestedAuthMethod) {
        boolean vtjForbiddenMethodRequested = requestedAuthMethod.equals(AuthMethod.KATSOPWD)
                || requestedAuthMethod.equals(AuthMethod.KATSOOTP) || requestedAuthMethod.equals(AuthMethod.MPASS1)
                || requestedAuthMethod.equals(AuthMethod.EIDAS1);
        VtjVerificationRequirement vtjVerificationRequirement;
        if (vtjForbiddenMethodRequested) {
            vtjVerificationRequirement = FORBIDDEN;
        } else if (requestedAuthMethod.equals(AuthMethod.HST)) {
            vtjVerificationRequirement = MUST_SUCCEED;
        } else {
            if (relyingParty.isVtjVerificationRequired())
                vtjVerificationRequirement = MUST_SUCCEED;
            else
                vtjVerificationRequirement = MAY_FAIL;
        }
        return vtjVerificationRequirement;
    }

    /**
     * Removes a session from session cache.
     *
     * @param tokenId token id, generated with built session phase Id handler
     * @param phaseId phase id, generated in session init phase
     * @param logTag log tag
     * @return Proxy message containing session conversation key.
     */
    public ProxyMessageDTO removeSessionById(String tokenId, String phaseId, String logTag) {
        logger.debug("<<{}>> Remove session from cache", logTag);
        ProxyMessageDTO message = new ProxyMessageDTO();
        try {
            if (phaseIdBuiltSession.validateTidAndPid(tokenId, phaseId) &&
                    phaseIdBuiltSession.verifyPhaseId(phaseId, tokenId, stepCancel)) {

                //This method is always called before authMethod is set to final value (use INIT)
                Session session = uidToUserSessionsCache.removeFromSessionCache(tokenId, AuthMethod.INIT);
                message.setConversationKey(session.getConversationKey());
                message.setErrorType(ErrorType.NO_ERROR);
            }
            else {
                logger.warn("<<{}>> Phase ID is invalid", logTag);
                message.setErrorType(ErrorType.PHASE_ID_FAILED);
            }
        } catch (Exception e) {
            logger.error("<<{}>> Error in verifying phase ID", logTag, e);
            message.setErrorType(ErrorType.INTERNAL_ERROR);
        }

        return message;
    }
}
