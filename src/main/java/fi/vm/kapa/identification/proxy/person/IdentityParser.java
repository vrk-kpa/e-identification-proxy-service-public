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
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Scope(value = "prototype")
public class IdentityParser {
    private final String SATU_DELIMITER;
    private static Map<Identifier.Types,String> identifierKeys = ImmutableMap.<Identifier.Types,String>builder()
            .put(Identifier.Types.HETU, "AJP_hetu")
            .put(Identifier.Types.SATU, "AJP_satu")
            .put(Identifier.Types.KID, "AJP_tfiKid")
            .put(Identifier.Types.EPPN, "AJP_eppn")
            .put(Identifier.Types.UID, "AJP_uid")
            .build();


    @Autowired
    public IdentityParser(@Value("${satu.issuer.delimiter}") String satuDelimiter) {
        SATU_DELIMITER = satuDelimiter;
    }

    public Map<Identifier.Types,String> parseIdentifiers(Map<String,String> spData) throws IdentityParsingException {
        Map<Identifier.Types,String> identifiers = new EnumMap<>(Identifier.Types.class);
        for (Identifier.Types identifierType: Identifier.Types.values()) {
            String key = identifierKeys.get(identifierType);
            if (spData.containsKey(key)) {
                if (Identifier.Types.SATU == identifierType) {
                    String[] identifierParts = getIdentifierParts(spData.get(key));
                    identifiers.put(identifierType, identifierParts[0]);
                } else {
                    identifiers.put(identifierType, spData.get(key));
                }
            }
        }
        return identifiers;
    }

    public Identity parse(Map<String,String> spData) throws IdentityParsingException {
        Identifier.Types identifierType = getIdentifierType(spData);
        String identifier = spData.get(identifierKeys.get(identifierType));
        switch (identifierType) {
            case HETU:
            case KID:
            case EPPN:
            case UID:
                return new Identity(null, identifierType, identifier);
            case SATU:
                String[] identifierParts = getIdentifierParts(identifier);
                return new Identity(identifierParts[1], identifierType, identifierParts[0]);
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

    public String[] getIdentifierParts(String identifierString) throws IdentityParsingException {
        if (identifierString.contains(SATU_DELIMITER)) {
            String[] satuIdentifierParts = identifierString.split(SATU_DELIMITER, 2);
            if (satuIdentifierParts.length == 2 &&
                    StringUtils.isNotBlank(satuIdentifierParts[0]) &&
                    StringUtils.isNotBlank(satuIdentifierParts[1])) {
                return satuIdentifierParts;
            } else {
                throw new IdentityParsingException("Invalid data with SATU delimiter");
            }
        } else {
            return new String[]{identifierString, null};
        }
    }
}
