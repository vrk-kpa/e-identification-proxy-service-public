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

import fi.vm.kapa.identification.proxy.exception.AttributeGenerationException;
import fi.vm.kapa.identification.type.SessionProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SessionAttributeCollector {

    private final String vtjRequiredKey;
    private final String vtjVerifiedKey;
    private final String vtjDataInvalidKey;

    private final String legacyVersionKey;
    private final String legacyVersionValue;

    @Autowired
    public SessionAttributeCollector(@Value("${saml.vtj.required.key}") String vtjRequiredKey,
                                     @Value("${saml.vtj.verified.key}") String vtjVerifiedKey,
                                     @Value("${saml.vtj.invalid.key}") String vtjDataInvalidKey,
                                     @Value("${legacy.version.key}") String legacyVersionKey,
                                     @Value("${legacy.version.value}") String legacyVersionValue) {
        this.vtjRequiredKey = vtjRequiredKey;
        this.vtjVerifiedKey = vtjVerifiedKey;
        this.vtjDataInvalidKey = vtjDataInvalidKey;
        this.legacyVersionKey = legacyVersionKey;
        this.legacyVersionValue = legacyVersionValue;
    }

    public Map<String,String> getAttributes(Session session) throws AttributeGenerationException {
        Map<String,String> attributes = new HashMap<>();
        attributes.put(vtjVerifiedKey, Boolean.toString(session.isVtjVerified()));
        attributes.put(vtjRequiredKey, Boolean.toString(session.isVtjVerificationRequired()));
        attributes.put(vtjDataInvalidKey, Boolean.toString(session.isVtjDataInvalid()));
        if (session.getSessionProfile() == SessionProfile.TUNNISTUSFI_LEGACY) {
            if (null != session.getAuthenticationProvider()) {
                attributes.put("provider", session.getAuthenticationProvider().getAuthProviderAuthContextUrl());
            }
            attributes.putAll(getLegacySessionAttributes(session));
            attributes.putAll(session.getIdentifiedPerson().getLegacyAttributes());
        } else if (session.getSessionProfile() == SessionProfile.VETUMA_SAML2
                && null != session.getAuthenticationProvider()) {
            attributes.put("provider", session.getAuthenticationProvider().getDbEntityIdAuthContextUrl());
        }

        attributes.putAll(getMergedPersonAttributes(session));
        return attributes;
    }

    Map<String,String> getMergedPersonAttributes(Session session) throws AttributeGenerationException {
        Map<String,String> personAttributes = new HashMap<>();
        if (null != session.getIdentifiedPerson()) {
            personAttributes.putAll(session.getIdentifiedPerson().getAttributes());
        }
        if (session.isVtjVerified() && null != session.getVtjPerson()) {
            personAttributes.putAll(session.getVtjPerson().getAttributes());
        }
        return personAttributes;
    }

    private Map<String,String> getLegacySessionAttributes(Session session) {
        Map<String,String> legacyAttributes = new HashMap<>();
        legacyAttributes.put(legacyVersionKey, legacyVersionValue);
        if (null != session.getLegacyVersion()) {
            legacyAttributes.put("legacyVersion", session.getLegacyVersion());
        }
        return legacyAttributes;
    }
}
