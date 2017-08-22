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

import fi.vm.kapa.identification.proxy.exception.IdentityParsingException;
import fi.vm.kapa.identification.proxy.exception.PersonParsingException;
import fi.vm.kapa.identification.type.AuthMethod;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Scope(value = "prototype")
public class IdentifiedPersonBuilder {

    private final KatsoPersonFactory katsoPersonFactory;
    private final GenericPersonFactory genericPersonFactory;
    private final EidasPersonFactory eidasPersonFactory;

    public IdentifiedPersonBuilder(KatsoPersonFactory katsoPersonFactory,
                                   GenericPersonFactory genericPersonFactory,
                                   EidasPersonFactory eidasPersonFactory) {
        this.katsoPersonFactory = katsoPersonFactory;
        this.genericPersonFactory = genericPersonFactory;
        this.eidasPersonFactory = eidasPersonFactory;
    }

    public IdentifiedPerson build(Map<String,String> spData, AuthMethod authenticationMethod) throws PersonParsingException {
        try {
            if (authenticationMethod == AuthMethod.KATSOPWD ||
                    authenticationMethod == AuthMethod.KATSOOTP) {
                return katsoPersonFactory.createFromSpData(spData);
            } else if (authenticationMethod == AuthMethod.EIDAS1) {
                return eidasPersonFactory.createFromSpData(spData);
            } else {
                return genericPersonFactory.createFromSpData(spData);
            }
        } catch (IdentityParsingException e) {
            throw new PersonParsingException("Can't parse session person", e);
        }
    }
}
