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
import fi.vm.kapa.identification.vtj.model.Person;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class VtjPersonAttributesTest {

    private Identity hetuIdentity = new Identity("", Identifier.Types.HETU, "TESTHETU");
    private Identity satuIdentity = new Identity("", Identifier.Types.SATU, "TEST_SATU");

    @Test
    public void getPersonAttributesSetsHetu() throws Exception {
        Person validPerson = getMinimalValidPerson("TESTHETU");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals(1, person.size());
        assertEquals("TESTHETU", person.get("samlNationalIdentificationNumber"));
    }

    @Test
    public void getPersonAttributesSetsProtectionOrder() throws Exception {
        Person validPerson = getMinimalValidPerson("TESTHETU");
        validPerson.setProtectionOrder(true);
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("1", person.get("samlProtectionOrder"));
    }

    @Test
    public void getPersonAttributesSetsFinnishCitizenship() throws Exception {
        Person validPerson = getMinimalValidPerson("TESTHETU");
        validPerson.setFinnishCitizenship("TEST");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST", person.get("samlFinnishCitizenship"));
    }

    @Test
    public void getPersonAttributesDoesNotSetNullFinnishCitizenship() throws Exception {
        Person validPerson = getMinimalValidPerson("TESTHETU");
        validPerson.setFinnishCitizenship(null);
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertFalse(person.containsKey("samlFinnishCitizenship"));
    }

    @Test
    public void getPersonAttributesSetsLastName() throws Exception {
        Person validPerson = getPersonWithLastnameAndFirstNames();
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_LASTNAME", person.get("samlSn"));
    }

    @Test
    public void getPersonAttributesSetsFirstName() throws Exception {
        Person validPerson = getPersonWithLastnameAndFirstNames();
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST FIRST NAMES", person.get("samlFirstName"));
        assertEquals("TEST", person.get("samlGivenName"));
        assertEquals("TEST TEST_LASTNAME", person.get("samlDisplayName"));
    }

    @Test
    public void getPersonAttributesSetsNickName() throws Exception {
        Person validPerson = getPersonWithLastnameAndFirstAndNickNames();
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST FIRST NAMES", person.get("samlFirstName"));
        assertEquals("TESTIE", person.get("samlGivenName"));
        assertEquals("TESTIE TEST_LASTNAME", person.get("samlDisplayName"));
    }

    @Test
    public void getPersonAttributesSetsXRoadCn() throws Exception {
        Person validPerson = getPersonWithLastnameAndFirstAndNickNames();
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals(validPerson.getLastName() + " " + validPerson.getFirstNames(), person.get("samlCn"));
    }

    @Test
    public void getPersonAttributesSetsEmailAddress() throws Exception {
        Person validPerson = getPersonWithEmailAddress("test@example.com");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("test@example.com", person.get("samlMail"));
    }

    @Test
    public void getPersonAttributesSetsMunicipalityCode() throws Exception {
        Person validPerson = getPersonWithMunicipalityCode("12345");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("12345", person.get("samlMunicipalityCode"));
    }

    @Test
    public void getPersonAttributesSetsMunicipalityUsingMunicipalityS() throws Exception {
        Person validPerson = getPersonWithMunicipalitySAndMunicipalityR("TEST_MUNICIPALITY", "SHOULD_NOT_BE_USED");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_MUNICIPALITY", person.get("samlMunicipality"));
    }

    @Test
    public void getPersonAttributesSetsMunicipalityUsingMunicipalityRWhenMunicipalitySIsBlank() throws Exception {
        Person validPerson = getPersonWithMunicipalitySAndMunicipalityR("", "TEST_MUNICIPALITY_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_MUNICIPALITY_R", person.get("samlMunicipality"));
    }

    @Test
    public void getPersonAttributesSetsDomesticAddressUsingDomesticAddressS() throws Exception {
        Person validPerson = getPersonWithDomesticAddressSAndDomesticAddressR("TEST_DOMESTIC_ADDRESS", "SHOULD_NOT_BE_USED");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_DOMESTIC_ADDRESS", person.get("samlDomesticAddress"));
    }

    @Test
    public void getPersonAttributesSetsDomesticAddressUsingDomesticAddressRWhenDomesticAddressSIsBlank() throws Exception {
        Person validPerson = getPersonWithDomesticAddressSAndDomesticAddressR("", "TEST_DOMESTIC_ADDRESS_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_DOMESTIC_ADDRESS_R", person.get("samlDomesticAddress"));
    }

    @Test
    public void getPersonAttributesSetsPostalCode() throws Exception {
        Person validPerson = getPersonWithPostalCode("12345");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("12345", person.get("samlPostalCode"));
    }

    @Test
    public void getPersonAttributesSetsCityUsingCityS() throws Exception {
        Person validPerson = getPersonWithCitySAndCityR("TEST_CITY", "SHOULD_NOT_BE_USED");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_CITY", person.get("samlCity"));
    }

    @Test
    public void getPersonAttributesSetsCityUsingCityRWhenCitySIsBlank() throws Exception {
        Person validPerson = getPersonWithCitySAndCityR("", "TEST_CITY_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_CITY_R", person.get("samlCity"));
    }

    @Test
    public void getPersonAttributesSetsForeignAddress() throws Exception {
        Person validPerson = getPersonWithForeignAddress("TEST_FOREIGN_ADDRESS");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_FOREIGN_ADDRESS", person.get("samlForeignAddress"));
    }

    @Test
    public void getPersonAttributesSetsForeignLocalityAndState() throws Exception {
        Person validPerson = getPersonWithForeignLocalityAndStateS_And_ForeignLocalityAndStateR("TEST_FOREIGN_LOCALITY_AND_STATE_S", "SHOULD_NOT_BE_USED");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_FOREIGN_LOCALITY_AND_STATE_S", person.get("samlForeignLocalityAndState"));
    }

    @Test
    public void getPersonAttributesSetsForeignLocalityAndStateUsingForeignLocalityRWhenForeignLocalitySIsBlank() throws Exception {
        Person validPerson = getPersonWithForeignLocalityAndStateS_And_ForeignLocalityAndStateR("", "TEST_FOREIGN_LOCALITY_AND_STATE_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_FOREIGN_LOCALITY_AND_STATE_R", person.get("samlForeignLocalityAndState"));
    }

    @Test
    public void getPersonAttributesSetsForeignLocalityAndStateClearText() throws Exception {
        Person validPerson = getPersonWithForeignLocalityAndStateClearText("TEST_FOREIGN_LOCALITY_CLEAR_TEXT");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_FOREIGN_LOCALITY_CLEAR_TEXT", person.get("samlForeignLocalityAndStateClearText"));
    }

    @Test
    public void getPersonAttributesSetsStateCode() throws Exception {
        Person validPerson = getPersonWithStateCode("TEST_STATE_CODE");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_STATE_CODE", person.get("samlState"));
    }

    @Test
    public void getPersonAttributesSetsTemporaryDomesticAddress() throws Exception {
        Person validPerson = getPersonWithTemporaryDomesticAddressSAndTemporaryDomesticAddressR("TEST_TEMPORARY_ADDRESS_S", "TEST_TEMPORARY_ADDRESS_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_TEMPORARY_ADDRESS_S", person.get("samlTemporaryDomesticAddress"));
    }

    @Test
    public void getPersonAttributesSetsTemporaryDomesticAddressUsingTemporaryDomesticAddressRWhenSIsBlank() throws Exception {
        Person validPerson = getPersonWithTemporaryDomesticAddressSAndTemporaryDomesticAddressR("", "TEST_TEMPORARY_ADDRESS_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_TEMPORARY_ADDRESS_R", person.get("samlTemporaryDomesticAddress"));
    }

    @Test
    public void getPersonAttributesSetsTemporaryPostalCode() throws Exception {
        Person validPerson = getPersonWithTemporaryPostalCode("TEST_POSTAL_CODE");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_POSTAL_CODE", person.get("samlTemporaryPostalCode"));
    }

    @Test
    public void getPersonAttributesSetsTemporaryCity() throws Exception {
        Person validPerson = getPersonWithTemporaryCitySAndTemporaryCityR("TEST_TEMPORARY_CITY_S", "TEST_TEMPORARY_CITY_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_TEMPORARY_CITY_S", person.get("samlTemporaryCity"));
    }

    @Test
    public void getPersonAttributesSetsTemporaryCityUsingTemporaryCityRWhenSIsBlank() throws Exception {
        Person validPerson = getPersonWithTemporaryCitySAndTemporaryCityR("", "TEST_TEMPORARY_CITY_R");
        Map<String, String> person = new VtjPerson(hetuIdentity, validPerson).getAttributes();
        assertEquals("TEST_TEMPORARY_CITY_R", person.get("samlTemporaryCity"));
    }

    @Test
    public void getPersonAttributesSetsSatu() throws Exception {
        Person validPerson = getPersonWithSatu("TEST_SATU");
        Map<String, String> person = new VtjPerson(satuIdentity, validPerson).getAttributes();
        assertEquals("TEST_SATU", person.get("samlElectronicIdentificationNumber"));
    }

    private Person getMinimalValidPerson(String hetu) {
        Person person = new Person();
        person.setHetu(hetu);
        person.setHetuValid(true);
        person.setProtectionOrder(false);
        person.setDeceased(false);
        return person;
    }

    private Person getPersonWithLastnameAndFirstNames() {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setLastName("TEST_LASTNAME");
        person.setFirstNames("TEST FIRST NAMES");
        return person;
    }

    private Person getPersonWithLastnameAndFirstAndNickNames() {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setLastName("TEST_LASTNAME");
        person.setFirstNames("TEST FIRST NAMES");
        person.setNickName("TESTIE");
        return person;
    }

    private Person getPersonWithEmailAddress(String emailAddress) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setEmailAddress(emailAddress);
        return person;
    }

    private Person getPersonWithMunicipalityCode(String municipalityCode) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setMunicipalityCode(municipalityCode);
        return person;
    }


    private Person getPersonWithMunicipalitySAndMunicipalityR(String municipalityS, String municipalityR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setMunicipalityS(municipalityS);
        person.setMunicipalityR(municipalityR);
        return person;
    }

    private Person getPersonWithDomesticAddressSAndDomesticAddressR(String domesticAddressS, String domesticAddressR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setDomesticAddressS(domesticAddressS);
        person.setDomesticAddressR(domesticAddressR);
        return person;
    }

    private Person getPersonWithPostalCode(String postalCode) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setPostalCode(postalCode);
        return person;
    }

    private Person getPersonWithCitySAndCityR(String cityS, String cityR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setCityS(cityS);
        person.setCityR(cityR);
        return person;
    }

    private Person getPersonWithForeignAddress(String foreignAddress) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setForeignAddress(foreignAddress);
        return person;
    }

    private Person getPersonWithForeignLocalityAndStateS_And_ForeignLocalityAndStateR(String testForeignLocalityAndStateS, String testForeignLocalityAndStateR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setForeignLocalityAndStateS(testForeignLocalityAndStateS);
        person.setForeignLocalityAndStateR(testForeignLocalityAndStateR);
        return person;
    }

    private Person getPersonWithForeignLocalityAndStateClearText(String testForeignLocalityClearText) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setForeignLocalityAndStateClearText(testForeignLocalityClearText);
        return person;
    }

    private Person getPersonWithStateCode(String stateCode) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setStateCode(stateCode);
        return person;
    }

    private Person getPersonWithTemporaryDomesticAddressSAndTemporaryDomesticAddressR(String testTemporaryAddressS, String testTemporaryAddressR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setTemporaryDomesticAddressS(testTemporaryAddressS);
        person.setTemporaryDomesticAddressR(testTemporaryAddressR);
        return person;
    }

    private Person getPersonWithTemporaryPostalCode(String testPostalCode) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setTemporaryPostalCode(testPostalCode);
        return person;
    }

    private Person getPersonWithTemporaryCitySAndTemporaryCityR(String testTemporaryCityS, String testTemporaryCityR) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setTemporaryCityS(testTemporaryCityS);
        person.setTemporaryCityR(testTemporaryCityR);
        return person;
    }

    private Person getPersonWithSatu(String testSatu) {
        Person person = getMinimalValidPerson("TESTHETU");
        person.setSatu(testSatu);
        person.setSatuValid(true);
        return person;
    }

}