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
package fi.vm.kapa.identification.proxy.service;

import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.proxy.person.GenericPerson;
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.proxy.vtj.VtjClient;
import fi.vm.kapa.identification.type.Identifier;
import fi.vm.kapa.identification.vtj.model.Person;
import fi.vm.kapa.identification.vtj.model.VTJResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class VtjPersonServiceTest {

    private final Identity identity = new Identity("any", Identifier.Types.HETU, "any");
    private IdentifiedPerson identifiedPerson;

    @Mock
    VtjClient vtjClient;

    @Autowired
    @InjectMocks
    private VtjPersonService personService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.HETU, "any");
        identifiedPerson = new GenericPerson(identity, null, null, null, null, identifiers);
    }

    @Test
    public void getVtjResponseReturnsPersonFromVtjClient() throws Exception {
        VTJResponse response = new VTJResponse();
        Person validPerson = getMinimalValidPerson("TESTHETU");
        response.setPerson(validPerson);
        when(vtjClient.fetchVtjData(identity)).thenReturn(response);

        VTJResponse vtjResponse = personService.getVtjResponse(identity);
        assertEquals(validPerson, vtjResponse.getPerson());
    }


    @Test(expected = VtjServiceException.class)
    public void getPersonDataFromVTJThrowsVtjServiceExceptionWhenNoPersonInfoIsReturnedFromVTJ() throws Exception {
        when(vtjClient.fetchVtjData(any(Identity.class))).thenReturn(new VTJResponse());
        personService.getVtjPerson(identifiedPerson);
    }

    private Person getMinimalValidPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setProtectionOrder(false);
        person.setDeceased(false);
        return person;
    }

}