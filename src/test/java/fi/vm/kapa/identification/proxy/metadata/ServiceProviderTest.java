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
package fi.vm.kapa.identification.proxy.metadata;

import fi.vm.kapa.identification.type.EidasSupport;
import fi.vm.kapa.identification.type.SessionProfile;
import org.junit.Test;

import static org.junit.Assert.*;

public class ServiceProviderTest {

    @Test
    public void getEntityId() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider("TEST_ENTITY_ID", null, null, null, false, "",EidasSupport.full, null);
        assertEquals("TEST_ENTITY_ID", serviceProvider.getEntityId());
    }

    @Test
    public void getLevelOfAssurance() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, "TEST_LOA", null, null, false,  "",EidasSupport.full, null);
        assertEquals("TEST_LOA", serviceProvider.getLevelOfAssurance());
    }

    @Test
    public void getPermittedAuthMethods() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, "TEST_PERMITTED_METHODS", null, false,  "",EidasSupport.full, null);
        assertEquals("TEST_PERMITTED_METHODS", serviceProvider.getPermittedAuthMethods());
    }

    @Test
    public void getSessionProfileReturnsSaml2() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, null, SessionProfile.VETUMA_SAML2, false,  "",EidasSupport.full, null);
        assertEquals(SessionProfile.VETUMA_SAML2, serviceProvider.getSessionProfile());
    }

    @Test
    public void getSessionProfileReturnsVetumaLegacy() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, null, SessionProfile.VETUMA_LEGACY, false,  "",EidasSupport.full, null);
        assertEquals(SessionProfile.VETUMA_LEGACY, serviceProvider.getSessionProfile());
    }

    @Test
    public void isVtjVerificationRequiredReturnsFalse() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, null, null, false,  "",EidasSupport.full, null);
        assertEquals(false, serviceProvider.isVtjVerificationRequired());
    }

    @Test
    public void isVtjVerificationRequiredReturnsTrue() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, null, null, true,  "",EidasSupport.full, null);
        assertEquals(true, serviceProvider.isVtjVerificationRequired());
    }

    @Test
    public void isAuthMethodListPermittedReturnsFalseCalledWithStringThatIsNotAuthMethod() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, "fLoA3;fLoA2", null, true,  "",EidasSupport.full, null);
        assertFalse(serviceProvider.isAuthMethodListPermitted("INIT"));
    }

    @Test
    public void isAuthMethodListPermittedReturnsFalseWhenRequestedWithNonPermittedMethod() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, "fLoA3;fLoA2", null, true,  "",EidasSupport.full, null);
        assertFalse(serviceProvider.isAuthMethodListPermitted("KATSOOTP"));
    }

    @Test
    public void isAuthMethodListPermittedReturnsTrueWhenRequestedWithPermittedMethod() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, "fLoA3;fLoA2", null, true,  "",EidasSupport.full, null);
        assertTrue(serviceProvider.isAuthMethodListPermitted("fLoA2"));
    }

    @Test
    public void isAuthMethodListPermittedIgnoresOrdering() throws Exception {
        ServiceProvider serviceProvider = new ServiceProvider(null, null, "fLoA3;fLoA2", null, true,  "",EidasSupport.full, null);
        assertTrue(serviceProvider.isAuthMethodListPermitted("fLoA2;fLoA3"));
    }


}