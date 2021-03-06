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
package fi.vm.kapa.identification.proxy.session;

import fi.vm.kapa.identification.proxy.exception.AttributeGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
public class SessionStatusPrinter {
    private static final Logger logger = LoggerFactory.getLogger(SessionStatusPrinter.class);

    private SessionAttributeCollector sessionAttributeCollector;

    @Autowired
    public SessionStatusPrinter(SessionAttributeCollector sessionAttributeCollector) {
        this.sessionAttributeCollector = sessionAttributeCollector;
    }

    public String printSessionStatus(Session session) {
        StringJoiner joiner = new StringJoiner("\n");

        joiner.add("-----CKEY: " + session.getConversationKey());
        joiner.add("------SELECTED METHOD ENTITYID: " + session.getSelectedAuthenticationProvider().getDbEntityIdAuthContextUrl());
        joiner.add("------RELYING PARTY: " + session.getRelyingPartyEntityId());
        try {
            sessionAttributeCollector.getAttributes(session).forEach(
                    (attrKey, attrValue) -> joiner.add("------------SESSIONATTR: " + attrKey + " VALUE: " + attrValue));
        } catch (AttributeGenerationException e) {
            logger.warn("failed to print session attributes, reason ", e);
        } catch (Exception e) {
            logger.warn("failed print session attributes, error in code. reason ", e);
        }
        joiner.add("------SESSIONPROFILE: " + (session.getSessionProfile() == null ? "null" :session.getSessionProfile().toString()));
        joiner.add("------TIMESTAMP: " + session.getTimestamp());
        joiner.add("------VALIDATED: " + session.isValidated());
        return joiner.toString();
    }
}
