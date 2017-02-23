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

public class KatsoPerson implements IdentifiedPerson {
    private final Identity identity;
    private final String commonName;
    private final Map<Identifier.Types,String> identifiers;

    public KatsoPerson(Identity identity, String commonName, Map<Identifier.Types,String> identifiers) {
        this.identity = identity;
        this.commonName = commonName;
        this.identifiers = identifiers;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public String getCommonName() {
        return commonName;
    }

    @Override
    public Map<Identifier.Types,String> getIdentifiers() {
        return identifiers;
    }

    @Override
    public Map<String,String> getAttributes() throws AttributeGenerationException {
        Map<String,String> attributes = new HashMap<>();
        for (Map.Entry<Identifier.Types,String> entry: getIdentifiers().entrySet()) {
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
                case EPPN:
                    attributes.put("samlEppn", entry.getValue());
                    break;
                case UID:
                    attributes.put("samlUid", entry.getValue());
                    break;
                default:
                    throw new AttributeGenerationException("Unknown type:" + entry.getKey() + ", value:" + entry.getValue());
            }
        }
        attributes.put("samlCn", getCommonName());
        return attributes;
    }

}
