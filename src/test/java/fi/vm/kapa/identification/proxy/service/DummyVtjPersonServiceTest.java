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

import fi.vm.kapa.identification.proxy.person.GenericPerson;
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;
import fi.vm.kapa.identification.vtj.model.VtjIssue;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static fi.vm.kapa.identification.type.Identifier.Types.SATU;
import static org.junit.Assert.assertEquals;

public class DummyVtjPersonServiceTest {

    DummyVtjPersonService personService;

    @Before
    public void setUp() throws Exception {
        personService = new DummyVtjPersonService("010191-9696");
    }

    @Test
    public void getVtjPersonReturnsValidPersonForPersonIdentifiedByHetu() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithHetu("010191-9641");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        vtjPerson.validate();
    }

    @Test
    public void getVtjPersonReturnsPersonWithHetuFromIdentifiedPerson() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithHetu("010191-9641");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        assertEquals("010191-9641", vtjPerson.getAttributes().get("samlNationalIdentificationNumber"));
    }

    @Test
    public void getVtjPersonReturnsValidPersonForPersonIdentifiedBySatu() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithSatu("999196993");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        vtjPerson.validate();
    }

    @Test
    public void getVtjPersonReturnsPersonWithSatuFromIdentifiedPersonWhenSatuIdentifier() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithSatu("999196993");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        assertEquals("999196993", vtjPerson.getAttributes().get("samlElectronicIdentificationNumber"));
    }

    @Test
    public void getVtjPersonReturnsPersonWithDefaultHetuWhenHetuNotGiven() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithSatu("999196993");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        assertEquals("010191-9696", vtjPerson.getAttributes().get("samlNationalIdentificationNumber"));
    }

    @Test
    public void getVtjPersonReturnsPersonWithHetuFromIdentifiedPersonWhenSatuIdentifier() throws Exception {
        IdentifiedPerson person = getIdentifiedPersonWithSatu("999196993");
        person.getIdentifiers().put(HETU, "010191-9641");
        VtjPerson vtjPerson = personService.getVtjPerson(person, new VtjIssue());
        assertEquals("010191-9641", vtjPerson.getAttributes().get("samlNationalIdentificationNumber"));
    }


    IdentifiedPerson getIdentifiedPersonWithHetu(String hetu) {
        Identity identity = new Identity(null, HETU, hetu);
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.HETU, hetu);
        return new GenericPerson(identity, null, null, null, null, identifiers);
    }

    IdentifiedPerson getIdentifiedPersonWithSatu(String satu) {
        Identity identity = new Identity("VRK", SATU, satu);
        Map<Identifier.Types,String> identifiers = new HashMap<>();
        identifiers.put(Identifier.Types.SATU, satu);
        return new GenericPerson(identity, null, null, null, null, identifiers);
    }
}
