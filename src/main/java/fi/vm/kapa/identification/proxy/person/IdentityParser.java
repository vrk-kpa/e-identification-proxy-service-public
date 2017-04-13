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

import fi.vm.kapa.identification.proxy.exception.IdentityParsingException;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class IdentityParser {
    private static Map<Identifier.Types,String> identifierKeys = ImmutableMap.<Identifier.Types,String>builder()
            .put(Identifier.Types.HETU, "AJP_hetu")
            .put(Identifier.Types.SATU, "AJP_satu")
            .put(Identifier.Types.KID, "AJP_tfiKid")
            .put(Identifier.Types.EPPN, "AJP_eppn")
            .put(Identifier.Types.UID, "AJP_uid")
            .put(Identifier.Types.EIDAS_ID, "AJP_eidasPersonIdentifier")
            .build();

    public Map<Identifier.Types,String> parseIdentifiers(Map<String,String> spData) throws IdentityParsingException {
        Map<Identifier.Types,String> identifiers = new EnumMap<>(Identifier.Types.class);
        for (Identifier.Types identifierType: Identifier.Types.values()) {
            String key = identifierKeys.get(identifierType);
            if (spData.containsKey(key)) {
                identifiers.put(identifierType, spData.get(key));
            }
        }
        return identifiers;
    }

    public Identity parse(Map<String,String> spData) throws IdentityParsingException {
        Identifier.Types identifierType = getIdentifierType(spData);
        String identifier = spData.get(identifierKeys.get(identifierType));
        String issuerCN = spData.get("AJP_issuerCN");
        switch (identifierType) {
            case HETU:
            case KID:
            case EPPN:
            case UID:
            case EIDAS_ID:
                return new Identity(null, identifierType, identifier);
            case SATU:
                return new Identity(issuerCN, identifierType, identifier);
            default:
                throw new IdentityParsingException("Unknown Identifier type " + identifierType);
        }
    }

    public Identifier.Types getIdentifierType(Map<String,String> spData) throws IdentityParsingException {
        String sessionIdentifierType = spData.get(Identifier.typeKey);
        if (null == sessionIdentifierType) {
            throw new IdentityParsingException("IdentifierType not found");
        }
        try {
            return Identifier.Types.valueOf(sessionIdentifierType);
        } catch (IllegalArgumentException e) {
            throw new IdentityParsingException("IdentifierType could not parsed from "
                    + sessionIdentifierType, e);
        }
    }

}
