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
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.proxy.vtj.VtjClient;
import fi.vm.kapa.identification.vtj.model.Person;
import fi.vm.kapa.identification.vtj.model.VTJResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RealVtjPersonService implements VtjPersonService {
    private static final Logger logger = LoggerFactory.getLogger(RealVtjPersonService.class);

    @Autowired
    VtjClient vtjClient;

    VTJResponse getVtjResponse(Identity identity) throws VtjServiceException {
        VTJResponse response = vtjClient.fetchVtjData(identity);
        if (response == null || response.getPerson() == null) {
            logger.error("VTJ response null or empty");
            throw new VtjServiceException("VTJ response null or empty");
        } else {
            return response;
        }
    }

    @Override
    public VtjPerson getVtjPerson(IdentifiedPerson identifiedPerson) throws VtjServiceException {
        Identity identity = identifiedPerson.getIdentity();
        Person person = getVtjResponse(identity).getPerson();
        return new VtjPerson(identity, person);
    }

}
