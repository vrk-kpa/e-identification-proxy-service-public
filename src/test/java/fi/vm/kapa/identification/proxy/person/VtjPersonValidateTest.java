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

import fi.vm.kapa.identification.proxy.exception.InvalidVtjDataException;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;
import fi.vm.kapa.identification.vtj.model.Person;
import org.junit.Test;

public class VtjPersonValidateTest {

    private static final String TEST_HETU = "TEST_HETU";
    private static final String TEST_SATU = "TEST_SATU";

    private final Identity identity = new Identity("any", Identifier.Types.HETU, TEST_HETU);
    private final Identity satuIdentity = new Identity("any", Identifier.Types.SATU, TEST_SATU);

    @Test
    public void validateDoesNotThrowForValidHetuPerson() throws Exception {
        Person person = getMinimalValidPerson(TEST_HETU);
        VtjPerson vtjPerson = new VtjPerson(identity, person);
        vtjPerson.validate();
    }

    @Test
    public void validateDoesNotThrowForValidSatuPerson() throws Exception {
        Person person = getMinimalValidSatuPerson(TEST_SATU);
        VtjPerson vtjPerson = new VtjPerson(satuIdentity, person);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void validateThrowsInvalidVtjDataExceptionWhenWrongHetu() throws Exception {
        Person person = getMinimalValidPerson("WRONG_HETU");
        VtjPerson vtjPerson = new VtjPerson(identity, person);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void validateThrowsInvalidVtjDataExceptionWhenNullPerson() throws Exception {
        VtjPerson vtjPerson = new VtjPerson(identity, null);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void validateThrowsInvalidVtjDataExceptionWhenNoHetu() throws Exception {
        Person person = getMinimalValidPerson(TEST_HETU);
        person.setHetu(null);
        VtjPerson vtjPerson = new VtjPerson(identity, person);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void validateThrowsInvalidVtjDataExceptionForPersonWithInvalidatedHetu() throws Exception {
        Person personWithInvalidatedHetu = getPersonWithInvalidatedHetu();
        VtjPerson vtjPerson = new VtjPerson(identity, personWithInvalidatedHetu);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void validateThrowsInvalidVtjDataExceptionForDeceasedPerson() throws Exception {
        Person deceasedPersonWithValidHetu = getDeceasedPerson();
        VtjPerson vtjPerson = new VtjPerson(identity, deceasedPersonWithValidHetu);
        vtjPerson.validate();
    }

    @Test(expected = InvalidVtjDataException.class)
    public void getPersonDataFromVTJThrowsInvalidVtjDataExceptionWhenPersonWithInvalidatedSatuIsReturnedFromVTJ() throws Exception {
        Identity satuIdentity = new Identity("any", Identifier.Types.SATU, "any");
        Person personWithInvalidatedSatu = getPersonWithInvalidatedSatu(satuIdentity);
        VtjPerson vtjPerson = new VtjPerson(satuIdentity, personWithInvalidatedSatu);
        vtjPerson.validate();
    }

    private Person getMinimalValidPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setProtectionOrder(false);
        person.setDeceased(false);
        return person;
    }

    private Person getMinimalValidSatuPerson(String satu) {
        Person person = getMinimalValidPerson("SOMEHETU");
        person.setSatu(satu);
        person.setSatuValid(true);
        return person;
    }


    private Person getPersonWithInvalidatedHetu() {
        Person person = getMinimalValidPerson(TEST_HETU);
        person.setHetu("invalid");
        person.setHetuValid(false);
        return person;
    }

    private Person getDeceasedPerson() {
        Person person = getMinimalValidPerson(TEST_HETU);
        person.setDeceased(true);
        return person;
    }

    private Person getPersonWithInvalidatedSatu(Identity satuIdentity) {
        Person person = getMinimalValidPerson(TEST_HETU);
        person.setSatu(satuIdentity.getIdentifier());
        person.setSatuValid(false);
        return person;
    }


}