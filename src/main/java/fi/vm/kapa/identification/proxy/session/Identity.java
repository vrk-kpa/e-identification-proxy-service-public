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

import fi.vm.kapa.identification.type.Identifier;

public class Identity {
    private final String issuerDn;
    private final Identifier.Types identifierType;
    private final String identifier;

    public Identity(String issuerDn, Identifier.Types identifierType, String identifier) {
        this.issuerDn = issuerDn;
        this.identifierType = identifierType;
        this.identifier = identifier;
    }

    public Identifier.Types getIdentifierType() {
        return identifierType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Identity identity = (Identity) o;

        if (issuerDn != null ? !issuerDn.equals(identity.issuerDn) : identity.issuerDn != null) return false;
        if (identifierType != identity.identifierType) return false;
        return identifier.equals(identity.identifier);

    }

    @Override
    public int hashCode() {
        int result = issuerDn != null ? issuerDn.hashCode() : 0;
        result = 31 * result + (identifierType != null ? identifierType.hashCode() : 0);
        result = 31 * result + identifier.hashCode();
        return result;
    }

}
