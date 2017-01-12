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
import fi.vm.kapa.identification.type.Identifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static fi.vm.kapa.identification.type.Identifier.Types.SATU;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

public class KatsoPersonFactoryTest {
    @Mock
    IdentifiedPersonParser identifiedPersonParser;

    @InjectMocks
    private KatsoPersonFactory personFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void factorySetsIdentityFromParserGetIdentity() throws Exception {
        Map<String,String> spData = new HashMap<>();
        Identity identity = new Identity("ISSUER", SATU, "TEST_SATU");
        when(identifiedPersonParser.getIdentity(anyMap())).thenReturn(identity);
        assertEquals(identity, personFactory.createFromSpData(spData).getIdentity());
    }

    @Test
    public void factorySetsIdentifiersFromParserGetIdentifiers() throws Exception {
        Map<String,String> spData = new HashMap<>();
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(HETU, "TEST_HETU");
        identifiers.put(SATU, "TEST_SATU");
        when(identifiedPersonParser.getIdentifiers(anyMap())).thenReturn(identifiers);
        assertTrue(personFactory.createFromSpData(spData).getIdentifiers().containsKey(HETU));
        assertEquals("TEST_HETU", personFactory.createFromSpData(spData).getIdentifiers().get(HETU));
        assertTrue(personFactory.createFromSpData(spData).getIdentifiers().containsKey(SATU));
        assertEquals("TEST_SATU", personFactory.createFromSpData(spData).getIdentifiers().get(SATU));
    }

    @Test
    public void factorySetsCommonNameFromParserGetCommonName() throws Exception {
        Map<String,String> spData = new HashMap<>();
        when(identifiedPersonParser.getCommonName(anyMap())).thenReturn("TEST_AJP_COMMON_NAME");
        assertEquals("TEST_AJP_COMMON_NAME", personFactory.createFromSpData(spData).getCommonName());
    }

}