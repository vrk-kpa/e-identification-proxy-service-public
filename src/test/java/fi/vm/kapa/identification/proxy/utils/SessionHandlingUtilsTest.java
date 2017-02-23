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
package fi.vm.kapa.identification.proxy.utils;

import java.util.HashMap;
import java.util.Map;

import fi.vm.kapa.identification.proxy.exception.AuthContextUrlMissingException;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.junit.Assert.assertEquals;

@ContextConfiguration(locations = "classpath:testContext.xml")
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class SessionHandlingUtilsTest {

    @Autowired
    private SessionHandlingUtils sessionHandlingUtils;

    public Map<String, String> getMapWithKeyAndValue(String key, String new_key) {
        Map<String, String> keysToNewKeys = new HashMap<>();
        keysToNewKeys.put(key, new_key);
        return keysToNewKeys;
    }

    @Test
    public void getSpProvidedEndIdPAuthContextUrlReturnsAuthContextClass() throws Exception {
        Map<String, String> spSessionData = new HashMap<>();
        spSessionData.put("AJP_Shib-AuthnContext-Class", "TEST_CONTEXT_CLASS");
        spSessionData.put("AJP_Shib-AuthnContext-Decl", "TEST_CONTEXT_DECL");
        String authContextUrl = sessionHandlingUtils.getSpProvidedEndIdPAuthContextUrl(spSessionData);
        assertEquals("TEST_CONTEXT_CLASS", authContextUrl);
    }

    @Test
    public void getSpProvidedEndIdPAuthContextUrlReturnsAuthContextDecl() throws Exception {
        Map<String, String> spSessionData = new HashMap<>();
        spSessionData.put("AJP_Shib-AuthnContext-Decl", "TEST_CONTEXT_DECL");
        String authContextUrl = sessionHandlingUtils.getSpProvidedEndIdPAuthContextUrl(spSessionData);
        assertEquals("TEST_CONTEXT_DECL", authContextUrl);
    }

    @Test(expected = AuthContextUrlMissingException.class)
    public void getSpProvidedEndIdPAuthContextUrlThrowsIfNeitherKeyFound() throws Exception {
        Map<String, String> spSessionData = new HashMap<>();
        sessionHandlingUtils.getSpProvidedEndIdPAuthContextUrl(spSessionData);
    }

}
