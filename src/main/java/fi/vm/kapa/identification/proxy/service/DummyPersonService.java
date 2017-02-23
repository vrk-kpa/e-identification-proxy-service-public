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
import fi.vm.kapa.identification.proxy.person.IdentifiedPerson;
import fi.vm.kapa.identification.proxy.person.VtjPerson;
import fi.vm.kapa.identification.vtj.model.Person;

import static fi.vm.kapa.identification.type.Identifier.Types.HETU;
import static fi.vm.kapa.identification.type.Identifier.Types.SATU;


public class DummyPersonService implements PersonService {

    public static final String DEFAULT_SATU_HETU = "010191-9696";

    @Override
    public VtjPerson getVtjPerson(IdentifiedPerson identifiedPerson) throws VtjServiceException {
        Person person = new Person();

        if (identifiedPerson.getIdentity().getIdentifierType() == HETU) {
            person.setHetu(identifiedPerson.getIdentity().getIdentifier());
            person.setHetuValid(true);
        }
        else if (identifiedPerson.getIdentity().getIdentifierType() == SATU) {
            person.setSatu(identifiedPerson.getIdentity().getIdentifier());
            person.setSatuValid(true);
            String hetu = identifiedPerson.getIdentifiers().getOrDefault(HETU, DEFAULT_SATU_HETU);
            person.setHetu(hetu);
            person.setHetuValid(true);
        }

        person.setFirstNames("vtj.ei.loydy.etunimet");
        person.setMunicipalityCode("vtj.ei.loydy.kuntakoodi");
        person.setMunicipalityS("vtj.ei.loydy.kunta");
        person.setDomesticAddressS("vtj.ei.loydy.kotimainenosoite");
        person.setPostalCode("vtj.ei.loydy.kotimainenPostinumero");
        person.setCityS("vtj.ei.loydy.kotimainenpostiToimipaikka");
        person.setForeignAddress("vtj.ei.loydy.ulkomainenOsoite");
        person.setForeignLocalityAndStateS("vtj.ei.loydy.ulkomainenPaikkaJaValtio");
        person.setForeignLocalityAndStateClearText("vtj.ei.loydy.ulkomainenPaikkaJaValtioSelkokielinen");
        person.setStateCode("vtj.ei.loydy.valtiokoodi");
        person.setTemporaryDomesticAddressS("vtj.ei.loydy.tilapainenKotimainenOsoite");
        person.setTemporaryPostalCode("vtj.ei.loydy.tilapainenKotimainenPostinumero");
        person.setTemporaryCityS("vtj.ei.loydy.tilapainenKotimainenPostiToimipaikka");

        return new VtjPerson(identifiedPerson.getIdentity(), person);
    }

}
