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

import fi.vm.kapa.identification.proxy.session.AuthMethodHelper;
import fi.vm.kapa.identification.type.SessionProfile;

public class ServiceProvider {
    private final String entityId;
    private final String levelOfAssurance;
    private final String permittedAuthMethods;
    private final SessionProfile sessionProfile;
    private final boolean vtjVerificationRequired;

    public ServiceProvider(String entityId,
                           String levelOfAssurance,
                           String permittedAuthMethods,
                           SessionProfile sessionProfile,
                           boolean vtjVerificationRequired) {
        this.entityId = entityId;
        this.levelOfAssurance = levelOfAssurance;
        this.permittedAuthMethods = permittedAuthMethods;
        this.sessionProfile = sessionProfile;
        this.vtjVerificationRequired = vtjVerificationRequired;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getLevelOfAssurance() {
        return levelOfAssurance;
    }

    public String getPermittedAuthMethods() {
        return permittedAuthMethods;
    }

    public SessionProfile getSessionProfile() {
        return sessionProfile;
    }

    public boolean isVtjVerificationRequired() {
        return vtjVerificationRequired;
    }

    public boolean isAuthMethodListPermitted(String requestedAuthenticationMethodStr) {
        return AuthMethodHelper.authMethodsInPermittedMethods(requestedAuthenticationMethodStr, permittedAuthMethods);
    }
}
