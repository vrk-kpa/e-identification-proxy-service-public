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
package fi.vm.kapa.identification.proxy.vtj;

import fi.vm.kapa.identification.proxy.exception.VtjServiceException;
import fi.vm.kapa.identification.proxy.session.Identity;
import fi.vm.kapa.identification.type.Identifier;
import fi.vm.kapa.identification.vtj.model.VTJResponse;
import fi.vm.kapa.identification.vtj.model.VtjIssue;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class VtjClientTest {

    @Test
    public void fetchVtjDataReturnsNullWhenVtjResponseIsNull() throws Exception {
        VtjClient vtjClient = spy(VtjClient.class);
        doReturn(null).when(vtjClient).getVtjResponseForUser(any(), any());
        VTJResponse vtjResponse = vtjClient.fetchVtjData(new Identity(" ", Identifier.Types.HETU, "TEST_HETU"),new VtjIssue());
        assertEquals(null, vtjResponse);
    }

    @Test(expected = VtjServiceException.class)
    public void fetchVtjDataThrowsVtjServiceException() throws Exception {
        VtjClient vtjClient = spy(VtjClient.class);
        when(vtjClient.getVtjResponseForUser(any(), any())).thenThrow(VtjServiceException.class);
    }

    @Test(expected = VtjServiceException.class)
    public void getVtjResponseForUserThrowsIfResponseStatusIsNot200() throws Exception {
        VtjClient vtjClient = spy(VtjClient.class);
        Response responseMock = mock(Response.class);
        doReturn(responseMock).when(vtjClient).getVtjHttpResponse(any(), any());
        when(responseMock.getStatus()).thenReturn(400);
        vtjClient.getVtjResponseForUser(new Identity("TEST_ISSUER_DN", Identifier.Types.SATU, "TEST_SATU"),new VtjIssue());
    }

    @Test
    public void getVtjResponseForUserReturnsVtjResponseWhenResponseStatusIs200() throws Exception {
        VtjClient vtjClient = spy(VtjClient.class);
        Response responseMock = mock(Response.class);
        Identity userIdentity = new Identity("TEST_ISSUER_DN", Identifier.Types.SATU, "TEST_SATU");
        VtjIssue vtjIssue = new VtjIssue();
        doReturn(responseMock).when(vtjClient).getVtjHttpResponse(userIdentity, vtjIssue);
        doReturn(new VTJResponse()).when(responseMock).readEntity(VTJResponse.class);
        doReturn(200).when(responseMock).getStatus();
        VTJResponse vtjResponse = vtjClient.getVtjResponseForUser(userIdentity, vtjIssue);
        assertNotNull(vtjResponse);
    }

}
