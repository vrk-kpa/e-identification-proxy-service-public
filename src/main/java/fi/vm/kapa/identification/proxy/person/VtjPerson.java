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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class VtjPerson {
    private static final Logger logger = LoggerFactory.getLogger(VtjPerson.class);

    private final Person person;
    private final Identity identity;

    public VtjPerson(Identity identity, Person person) {
        this.identity = identity;
        this.person = person;
    }

    public void validate() throws InvalidVtjDataException {
        if (null == person || StringUtils.isEmpty(person.getHetu())) {
            logger.error("Person not found from VTJ using identifier: " + identity.getIdentifier());
            throw new InvalidVtjDataException("Person not found from VTJ by identifier: " + identity.getIdentifier());
        }
        if (!person.isHetuValid()) {
            logger.error("Hetu is not valid");
            throw new InvalidVtjDataException("Hetu is not valid");
        }
        if (person.isDeceased()) {
            logger.error("Trying identify deceased person: " + identity.getIdentifier());
            throw new InvalidVtjDataException("Person is deceased");
        }
        if (StringUtils.isNotEmpty(person.getSatu()) &&
                !person.isSatuValid() && identity.getIdentifierType() == Identifier.Types.SATU) {
            logger.error("Person with invalid SATU: " + identity.getIdentifier());
            throw new InvalidVtjDataException("SATU is not valid");
        }
    }

    public Map<String,String> getAttributes() {
        Map<String,String> attributes = new HashMap<>();

        if (StringUtils.isNotEmpty(person.getHetu())) {
            attributes.put("samlNationalIdentificationNumber", person.getHetu());
        }
        if (StringUtils.isNotEmpty(person.getSatu())) {
            attributes.put("samlElectronicIdentificationNumber", person.getSatu());
        }
        attributes.put("samlProtectionOrder", person.isProtectionOrder() ? "1" : "0");
        if (StringUtils.isNotEmpty(person.getFinnishCitizenship())) {
            attributes.put("samlFinnishCitizenship", person.getFinnishCitizenship());
        }

        if (StringUtils.isNotEmpty(person.getLastName())) {
            attributes.put("samlSn", person.getLastName());
        }

        if (StringUtils.isNotEmpty(person.getFirstNames())) {
            attributes.put("samlFirstName", person.getFirstNames());
        }

        if (StringUtils.isNotEmpty(person.getNickName())) {
            attributes.put("samlGivenName", person.getNickName());
            if (StringUtils.isNotEmpty(person.getLastName())) {
                attributes.put("samlDisplayName", person.getNickName() + " " + person.getLastName());
                attributes.put("samlCn", person.getLastName() + " " + person.getFirstNames());
            }
        } else if (StringUtils.isNotBlank(person.getFirstNames())) {
            String delims = "[ ]+";
            attributes.put("samlGivenName", person.getFirstNames().split(delims)[0]);
            if (StringUtils.isNotEmpty(person.getLastName())) {
                attributes.put("samlDisplayName", person.getFirstNames().split(delims)[0] + " " + person.getLastName());
                attributes.put("samlCn", person.getLastName() + " " + person.getFirstNames());
            }
        }

        if (StringUtils.isNotEmpty(person.getEmailAddress())) {
            attributes.put("samlMail", person.getEmailAddress());
        }

        if (StringUtils.isNotEmpty(person.getMunicipalityCode())) {
            attributes.put("samlMunicipalityCode", person.getMunicipalityCode());
        }

        if (StringUtils.isNotEmpty(person.getMunicipalityS())) {
            attributes.put("samlMunicipality", person.getMunicipalityS());
        } else if (StringUtils.isNotEmpty(person.getMunicipalityR())) {
            attributes.put("samlMunicipality", person.getMunicipalityR());
        }

        if (StringUtils.isNotEmpty(person.getDomesticAddressS())) {
            attributes.put("samlDomesticAddress", person.getDomesticAddressS());
        } else if (StringUtils.isNotEmpty(person.getDomesticAddressR())) {
            attributes.put("samlDomesticAddress", person.getDomesticAddressR());
        }

        if (StringUtils.isNotEmpty(person.getPostalCode())) {
            attributes.put("samlPostalCode", person.getPostalCode());
        }

        if (StringUtils.isNotEmpty(person.getCityS())) {
            attributes.put("samlCity", person.getCityS());
        } else if (StringUtils.isNotEmpty(person.getCityR())) {
            attributes.put("samlCity", person.getCityR());
        }

        if (StringUtils.isNotEmpty(person.getForeignAddress())) {
            attributes.put("samlForeignAddress", person.getForeignAddress());
        }

        if (StringUtils.isNotEmpty(person.getForeignLocalityAndStateS())) {
            attributes.put("samlForeignLocalityAndState", person.getForeignLocalityAndStateS());
        } else if (StringUtils.isNotEmpty(person.getForeignLocalityAndStateR())) {
            attributes.put("samlForeignLocalityAndState", person.getForeignLocalityAndStateR());
        }

        if (StringUtils.isNotEmpty(person.getForeignLocalityAndStateClearText())) {
            attributes.put("samlForeignLocalityAndStateClearText", person.getForeignLocalityAndStateClearText());
        }

        if (StringUtils.isNotEmpty(person.getStateCode())) {
            attributes.put("samlState", person.getStateCode());
        }

        if (StringUtils.isNotEmpty(person.getTemporaryDomesticAddressS())) {
            attributes.put("samlTemporaryDomesticAddress", person.getTemporaryDomesticAddressS());
        } else if (StringUtils.isNotEmpty(person.getTemporaryDomesticAddressR())) {
            attributes.put("samlTemporaryDomesticAddress", person.getTemporaryDomesticAddressR());
        }

        if (StringUtils.isNotEmpty(person.getTemporaryPostalCode())) {
            attributes.put("samlTemporaryPostalCode", person.getTemporaryPostalCode());
        }

        if (StringUtils.isNotEmpty(person.getTemporaryCityS())) {
            attributes.put("samlTemporaryCity", person.getTemporaryCityS());
        } else if (StringUtils.isNotEmpty(person.getTemporaryCityR())) {
            attributes.put("samlTemporaryCity", person.getTemporaryCityR());
        }
        return attributes;
    }

}
