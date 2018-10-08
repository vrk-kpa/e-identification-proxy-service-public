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

import java.util.HashMap;
import java.util.Map;

public class GenericPerson implements IdentifiedPerson {

    private final Identity identity;
    private final Map<Identifier.Types,String> identifiers;

    private final String commonName;
    private final String surname;
    private final String givenName;
    private final String mobileNumber;

    public GenericPerson(Identity identity, String commonName, String surname, String givenName, String mobileNumber, Map<Identifier.Types,String> identifiers) {
        this.identity = identity;
        this.commonName = commonName;
        this.surname = surname;
        this.givenName = givenName;
        this.mobileNumber = mobileNumber;
        this.identifiers = identifiers;
    }


    @Override
    public Identity getIdentity() {
        return identity;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getSurname() {
        return surname;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    @Override
    public Map<Identifier.Types,String> getIdentifiers() {
        return identifiers;
    }

    @Override
    public Map<String,String> getAttributes() throws AttributeGenerationException {
        Map<String,String> attributes = new HashMap<>();
        for (Map.Entry<Identifier.Types,String> entry : getIdentifiers().entrySet()) {
            switch (entry.getKey()) {
                case HETU:
                    attributes.put("samlNationalIdentificationNumber", entry.getValue());
                    break;
                case SATU:
                    attributes.put("samlElectronicIdentificationNumber", entry.getValue());
                    break;
                case KID:
                    attributes.put("samlKid", entry.getValue());                   
                    break;
                default:
                    throw new AttributeGenerationException("Unknown type:" + entry.getKey() + ", value:" + entry.getValue());
            }
        }
        putIfNonEmpty(attributes, "samlCn", getCommonName());
        putIfNonEmpty(attributes, "samlGivenName", getGivenName());
        putIfNonEmpty(attributes, "samlSn", getSurname());
        putIfNonEmpty(attributes, "samlMobile", getMobileNumber());
        return attributes;
    }

    @Override
    public Map<String,String> getLegacyAttributes() throws AttributeGenerationException {
        Map<String,String> legacyAttributes = new HashMap<>();
        if (getIdentity().getIdentifierType() == Identifier.Types.KID) {
            legacyAttributes.put("legacyKid", getIdentity().getIdentifier());
        } else {
            legacyAttributes.put("legacyPin", getIdentity().getIdentifier());
        }
        legacyAttributes.put("legacyPersonName", getCommonName());
        return legacyAttributes;
    }

    void putIfNonEmpty(Map<String,String> attributes, String key, String value) {
        if (null != value && !value.isEmpty()) {
            attributes.put(key, value);
        }
    }

}
