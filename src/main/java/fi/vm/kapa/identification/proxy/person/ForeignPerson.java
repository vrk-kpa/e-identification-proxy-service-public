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
package fi.vm.kapa.identification.proxy.person;

import fi.vm.kapa.identification.proxy.exception.AttributeGenerationException;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ForeignPerson implements IdentifiedPerson {

    private final String familyName;
    private final String firstNames;
    private final String dateOfBirth;
    private final String identityAssuranceLevel;
    private final Identity identity;
    private final Map<Identifier.Types,String> identifiers;

    public ForeignPerson(String familyName, String firstNames, String dateOfBirth, String ial, Identity identity, Map<Identifier.Types,String> identifiers) {
        this.familyName = familyName;
        this.firstNames = firstNames;
        this.dateOfBirth = dateOfBirth;
        this.identityAssuranceLevel = ial;
        this.identity = identity;
        this.identifiers = identifiers;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return firstNames;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getIdentityAssuranceLevel() {
        return identityAssuranceLevel;
    }

    public Identity getIdentity() {
        return identity;
    }

    public Map<Identifier.Types,String> getIdentifiers() {
        return identifiers;
    }

    @Override
    public Map<String,String> getAttributes() throws AttributeGenerationException {
        Map<String,String> attributes = new HashMap<>();
        for (Map.Entry<Identifier.Types,String> entry : getIdentifiers().entrySet()) {
            switch (entry.getKey()) {
                case FPID:
                    attributes.put("samlForeignPersonIdentifier", entry.getValue());
                    break;
                default:
                    throw new AttributeGenerationException("Unknown type:" + entry.getKey() + ", value:" + entry.getValue());
            }
        }
        putIfNonEmpty(attributes, "samlFirstName", getGivenName());
        putIfNonEmpty(attributes, "samlSn", getFamilyName());
        putIfNonEmpty(attributes, "samlDateOfBirth", getDateOfBirth());
        putIfNonEmpty(attributes, "samlIdentityAssuranceLevel", getIdentityAssuranceLevel());
        return attributes;
    }

    @Override
    public Map<String,String> getLegacyAttributes() throws AttributeGenerationException {
        return Collections.emptyMap();
    }

    void putIfNonEmpty(Map<String,String> attributes, String key, String value) {
        if (null != value && !value.isEmpty()) {
            attributes.put(key, value);
        }
    }
}
