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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ForeignPersonParser {
    private final IdentityParser identityParser;

    @Autowired
    public ForeignPersonParser(IdentityParser identityParser) {
        this.identityParser = identityParser;
    }

    String getFirstNames(Map<String,String> spData) {
        return spData.get("AJP_eidasGivenName");
    }

    String getDateOfBirth(Map<String,String> spData) {
        return spData.get("AJP_eidasDateOfBirth");
    }

    String getFamilyName(Map<String,String> spData) {
        return spData.get("AJP_sn");
    }

    Identity getIdentity(Map<String,String> spData) throws IdentityParsingException {
        return identityParser.parse(spData);
    }

    Map<Identifier.Types,String> getIdentifiers(Map<String,String> spData) throws IdentityParsingException {
        return identityParser.parseIdentifiers(spData);
    }

}
