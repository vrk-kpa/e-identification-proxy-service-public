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
package fi.vm.kapa.identification.proxy.session;

import fi.vm.kapa.identification.proxy.metadata.AuthenticationProvider;
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.SessionProfile;

import java.util.Set;

/**
 * This class is used to store any session related data. This class is mainly used
 * by Proxy Server but this can be used by other components as well. The raw session
 * data is stored into a simple map with key-value pairs and this class must not
 * implement any business logic related to that data.
 */
public class Session {

    private String uid;

    private String conversationKey;

    private boolean validated;

    private boolean vtjDataInvalid;

    private boolean vtjVerified;

    private boolean vtjVerificationRequired;

    private long timestamp;

    private String relyingPartyEntityId;

    private SessionProfile sessionProfile;
    private AuthenticationProvider authenticationProvider;
    private String legacyVersion;

    //holds all requested authentication methods
    private Set<AuthMethod> requestedAuthenticationMethods;

    private IdentifiedPerson identifiedPerson;
    private VtjPerson vtjPerson;

    // Setters and getters

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getConversationKey() {
        return conversationKey;
    }

    public void setConversationKey(String conversationKey) {
        this.conversationKey = conversationKey;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public boolean isVtjDataInvalid() {
        return vtjDataInvalid;
    }

    public void setVtjDataInvalid(boolean vtjDataInvalid) {
        this.vtjDataInvalid = vtjDataInvalid;
    }

    public boolean isVtjVerified() {
        return vtjVerified;
    }

    public void setVtjVerified(boolean vtjVerified) {
        this.vtjVerified = vtjVerified;
    }

    public boolean isVtjVerificationRequired() {
        return vtjVerificationRequired;
    }

    public void setVtjVerificationRequired(boolean vtjVerificationRequired) {
        this.vtjVerificationRequired = vtjVerificationRequired;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    public SessionProfile getSessionProfile() {
        return sessionProfile;
    }

    public void setSessionProfile(SessionProfile sessionProfile) {
        this.sessionProfile = sessionProfile;
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public void setAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public Set<AuthMethod> getRequestedAuthenticationMethods() {
        return requestedAuthenticationMethods;
    }

    public void setRequestedAuthenticationMethods(Set<AuthMethod> requestedAuthenticationMethods) {
        this.requestedAuthenticationMethods = requestedAuthenticationMethods;
    }

    public String getRelyingPartyEntityId() {
        return relyingPartyEntityId;
    }

    public void setRelyingPartyEntityId(String relyingPartyEntityId) {
        this.relyingPartyEntityId = relyingPartyEntityId;
    }

    public void setIdentifiedPerson(IdentifiedPerson identifiedPerson) {
        this.identifiedPerson = identifiedPerson;
    }

    public IdentifiedPerson getIdentifiedPerson() {
        return identifiedPerson;
    }

    public void setVtjPerson(VtjPerson vtjPerson) {
        this.vtjPerson = vtjPerson;
    }

    public VtjPerson getVtjPerson() {
        return vtjPerson;
    }

    public String getLegacyVersion() {
        return legacyVersion;
    }

    public void setLegacyVersion(String legacyVersion) {
        this.legacyVersion = legacyVersion;
    }
}
