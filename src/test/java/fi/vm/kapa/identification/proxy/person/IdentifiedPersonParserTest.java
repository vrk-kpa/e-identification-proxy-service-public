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

import fi.vm.kapa.identification.proxy.session.Identity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

public class IdentifiedPersonParserTest {
    @Mock
    IdentityParser identityParser;

    @InjectMocks
    IdentifiedPersonParser identifiedPersonParser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getIdentity() throws Exception {
        when(identifiedPersonParser.getIdentity(anyMap())).thenReturn(new Identity(null, HETU, "000000-0000"));
        assertEquals("000000-0000", identifiedPersonParser.getIdentity(new HashMap<>()).getIdentifier());
        assertEquals(HETU, identifiedPersonParser.getIdentity(new HashMap<>()).getIdentifierType());
    }

    @Test
    public void parserGetsCommonNameFromAjpCn() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put("AJP_cn", "TEST_AJP_CN");
        assertEquals("TEST_AJP_CN", identifiedPersonParser.getCommonName(spData));
    }

    @Test
    public void parserGetsCommonNameFromTfiPersonNameWhenAjpCnNotInMap() throws Exception {
        Map<String,String> spData = new HashMap<>();
        spData.put("AJP_tfiPersonName", "TEST_AJP_TFI_PERSON_NAME");
        assertEquals("TEST_AJP_TFI_PERSON_NAME", identifiedPersonParser.getCommonName(spData));
    }

}