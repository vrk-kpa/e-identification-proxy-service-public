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

import fi.vm.kapa.identification.type.AuthMethod;

public class AuthenticationProvider {

    private final String name;
    private final String domainName;
    private final String realMethod; // real authentication method, db: attr_loa
    private final AuthMethod authenticationMethod; // LoA, db: loa
    private final String authProviderAuthContextUrl;
    private final String dbEntityIdAuthContextUrl;
    private final String loginContext;

    public AuthenticationProvider(
            String name,
            String domainName,
            String realMethod,
            AuthMethod authenticationMethod,
            String authProviderAuthContextUrl,
            String dbEntityIdAuthContextUrl,
            String loginContext) {
        this.name = name;
        this.domainName = domainName;
        this.realMethod = realMethod;
        this.authenticationMethod = authenticationMethod;
        this.authProviderAuthContextUrl = authProviderAuthContextUrl;
        this.dbEntityIdAuthContextUrl = dbEntityIdAuthContextUrl;
        this.loginContext = loginContext;
    }

    public String getName() {
        return name;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getRealMethod() {
        return realMethod;
    }

    public AuthMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public String getAuthProviderAuthContextUrl() {
        return authProviderAuthContextUrl;
    }

    public String getDbEntityIdAuthContextUrl() {
        return dbEntityIdAuthContextUrl;
    }

    public String getLoginContext() {
        return loginContext;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) {
            return false;
        }
        if (!AuthenticationProvider.class.isAssignableFrom(obj.getClass())) {
            return false;
        }

        return this.dbEntityIdAuthContextUrl.equals(((AuthenticationProvider) obj).dbEntityIdAuthContextUrl);
    }
}
