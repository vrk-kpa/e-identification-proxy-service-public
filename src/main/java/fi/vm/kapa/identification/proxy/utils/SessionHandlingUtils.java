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

import fi.vm.kapa.identification.proxy.exception.AuthContextUrlMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import java.util.Map;

@Service
@Singleton
public class SessionHandlingUtils {
    private static final Logger logger = LoggerFactory.getLogger(SessionHandlingUtils.class);

    public String getSpProvidedEndIdPAuthContextUrl(Map<String,String> spSessionData) throws AuthContextUrlMissingException {
        String spProvidedEndIdPAuthContextUrl;
        if (spSessionData.get("AJP_Shib-AuthnContext-Class") != null) {
            spProvidedEndIdPAuthContextUrl = spSessionData.get("AJP_Shib-AuthnContext-Class");
        } else if (spSessionData.get("AJP_Shib-AuthnContext-Decl") != null) {
            spProvidedEndIdPAuthContextUrl = spSessionData.get("AJP_Shib-AuthnContext-Decl");
        } else {
            logger.error("Error building new session: used authentication method does not exist ");
            throw new AuthContextUrlMissingException("Error building new session: used authentication method does not exist ");
        }
        return spProvidedEndIdPAuthContextUrl;
    }

    public String getLegacyVersion(Map<String,String> spSessionData) {
        return spSessionData.get("AJP_tfiVersion");
    }

}
