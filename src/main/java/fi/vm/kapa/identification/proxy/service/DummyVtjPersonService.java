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

import fi.vm.kapa.identification.proxy.exception.InvalidVtjDataException;
import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.service.PersonService;
import fi.vm.kapa.identification.test.DummyPersonService;
import fi.vm.kapa.identification.vtj.model.Person;
import fi.vm.kapa.identification.vtj.model.VtjIssue;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static fi.vm.kapa.identification.type.Identifier.Types.SATU;

public class DummyVtjPersonService implements VtjPersonService {
    private final PersonService personService;

    public DummyVtjPersonService(String hetu) {
        this.personService = new DummyPersonService(hetu);
    }

    @Override
    public VtjPerson getVtjPerson(IdentifiedPerson identifiedPerson, VtjIssue vtjIssue) throws VtjServiceException, InvalidVtjDataException {
        Identity identity = identifiedPerson.getIdentity();
        Person person = personService.getPerson(identity.getIdentifier(), identity.getIdentifierType());
        // if SATU identity, but HETU in identifiers, replace returned HETU
        if (identity.getIdentifierType() == SATU) {
            identifiedPerson.getIdentifiers().forEach(
                    (type, identifier) -> {
                        if (type == HETU) person.setHetu(identifier);
                    }
            );
        }
        return new VtjPerson(identity, person);
    }

}
