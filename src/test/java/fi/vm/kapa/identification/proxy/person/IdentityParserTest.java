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

import static fi.vm.kapa.identification.type.Identifier.Types.FPID;
import static fi.vm.kapa.identification.type.Identifier.Types.SATU;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;

public class IdentityParserTest {

    private IdentityParser identityParser = new IdentityParser();

    @Test
    public void parseIdentifiersSatu() throws Exception {
        HashMap<Identifier.Types,String> expected = new HashMap<>();
        expected.put(SATU, "TEST_SATU");
        HashMap<String,String> spData = new HashMap<>();
        spData.put("AJP_satu", "TEST_SATU");
        spData.put("AJP_issuerCN", "TEST_ORGANIZATION");
        Map<Identifier.Types,String> identifiers = identityParser.parseIdentifiers(spData);
        assertThat(identifiers, equalTo(expected));
    }
    
    @Test
    public void parseIdentifiersForeign() throws Exception {
        HashMap<Identifier.Types,String> expected = new HashMap<>();
        expected.put(FPID, "TEST_FOREIGN");
        HashMap<String,String> spData = new HashMap<>();
        spData.put("AJP_foreignPersonIdentifier", "TEST_FOREIGN");
        Map<Identifier.Types,String> identifiers = identityParser.parseIdentifiers(spData);
        assertThat(identifiers, equalTo(expected));
    }

    @Test
    public void parseSatu() throws Exception {
        HashMap<String,String> spData = new HashMap<>();
        spData.put("AJP_satu", "TEST_SATU");
        spData.put("AJP_issuerCN", "TEST_ORGANIZATION");
        spData.put("identifierType", SATU.name());
        Identity identity = identityParser.parse(spData);
        assertEquals("TEST_ORGANIZATION", identity.getIssuerDn());
        assertEquals(SATU, identity.getIdentifierType());
        assertEquals("TEST_SATU", identity.getIdentifier());
    }

}