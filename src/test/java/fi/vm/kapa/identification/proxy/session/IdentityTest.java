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

import fi.vm.kapa.identification.proxy.person.IdentityParser;
import fi.vm.kapa.identification.type.Identifier;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class IdentityTest {
    @Test
    public void builderSetsNationalIdentificationNumberFromAjpHetu() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put(Identifier.typeKey, "HETU");
        spData.put("AJP_hetu", "TEST_AJP_HETU");
        spData.put("AJP_satu", "TEST_AJP_SATU");
        assertEquals("TEST_AJP_HETU", new IdentityParser("E_VRK_ID").parse(spData).getIdentifier());
    }

    @Test
    public void builderSetsElectronicIdentificationNumberFromAjpSatu() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put(Identifier.typeKey, "SATU");
        spData.put("AJP_satu", "TEST_AJP_SATU");
        assertEquals("TEST_AJP_SATU", new IdentityParser("E_VRK_ID").parse(spData).getIdentifier());
    }

    @Test
    public void builderSetsKidFromAjpTfiKid() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put(Identifier.typeKey, "KID");
        spData.put("AJP_tfiKid", "TEST_AJP_KID");
        assertEquals("TEST_AJP_KID", new IdentityParser("E_VRK_ID").parse(spData).getIdentifier());
    }

    @Test
    public void builderSetsEppnFromAjpEppn() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put(Identifier.typeKey, "EPPN");
        spData.put("AJP_eppn", "TEST_AJP_EPPN");
        assertEquals("TEST_AJP_EPPN", new IdentityParser("E_VRK_ID").parse(spData).getIdentifier());
    }


}