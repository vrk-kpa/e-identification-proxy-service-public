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
import fi.vm.kapa.identification.type.AuthMethod;
import fi.vm.kapa.identification.type.Identifier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

public class IdentifiedPersonBuilderTest {

    @Mock
    KatsoPersonFactory katsoPersonFactory;

    @Mock
    GenericPersonFactory genericPersonFactory;

    @Autowired
    @InjectMocks
    private IdentifiedPersonBuilder identifiedPersonBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Identity identity = new Identity(null, HETU, "0000000");
        HashMap<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(HETU, "0000000");
        when(katsoPersonFactory.createFromSpData(anyMap())).thenReturn(new KatsoPerson(identity, "TEST COMMON NAME", identifiers));
        when(genericPersonFactory.createFromSpData(anyMap())).thenReturn(new GenericPerson(identity, "TEST COMMON NAME", identifiers));
    }

    @Test
    public void buildReturnsKatsoPersonForKatsoPwdSession() throws Exception {
        assertTrue(identifiedPersonBuilder.build(null, AuthMethod.KATSOPWD) instanceof KatsoPerson);
    }

    @Test
    public void buildReturnsKatsoPersonForKatsoOtpSession() throws Exception {
        assertTrue(identifiedPersonBuilder.build(null, AuthMethod.KATSOOTP) instanceof KatsoPerson);
    }

    @Test
    public void buildReturnsGenericPersonForLoa2Session() throws Exception {
        assertTrue(identifiedPersonBuilder.build(null, AuthMethod.fLoA2) instanceof GenericPerson);
    }

    @Test
    public void buildReturnsGenericPersonForLoa3Session() throws Exception {
        assertTrue(identifiedPersonBuilder.build(null, AuthMethod.fLoA3) instanceof GenericPerson);
    }
}
