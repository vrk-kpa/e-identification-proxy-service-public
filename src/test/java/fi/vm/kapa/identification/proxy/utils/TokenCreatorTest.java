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

import com.auth0.jwt.JWT;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import java.util.Calendar;
import java.util.Date;

@ContextConfiguration(locations = "classpath:testContext.xml")
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
@RunWith(SpringJUnit4ClassRunner.class)
public class TokenCreatorTest {

    @Autowired
    private TokenCreator tokenCreator;

    @Test
    public void testTokenCreation() throws Exception
    {
        String jwt = tokenCreator.getAuthenticationToken("hetu","method","rp","session_id","request_id",new Date(1492775862));
        String expected = "eyJhbGciOiJSUzI1NiIsImtpZCI6IlN1b21pLmZpLXR1bm5pc3R1cyJ9.eyJyZXFfaWQiOiJyZXF1ZXN0X2lkIiwiYXV0aF9tZXRob2QiOiJtZXRob2QiLCJpc3MiOiJTdW9taS5maS10dW5uaXN0dXMiLCJleHAiOjE1NTI3NzUsImlhdCI6MTQ5Mjc3NSwiaGV0dSI6ImhldHUiLCJycCI6InJwIiwic2ZpX2lkIjoic2Vzc2lvbl9pZCJ9.RgZsDNHTysipLr36zDiG0f9auIg6WDefaGMLjR1qVizYV3D4oE1ZT8xawNIPr9kiNSkRpMww13kbFIaL4s96atmH-UaVJfGpd8ym_xywKo9su9hyw_61sn_JO3x0WIyAEXSwLyxpp9maoC_5rhDj5yK8eeYKR4mZU4WXDGH7f5-_jb453R5Dw7YBAWV35Upavewkxji5P4f9j80aBZQfanyjWNCuFKc1BZ-4PZEFLET0x89ph4FiE-qO2-Y9YMZ_K6ZhFUUXJwMtvCIbrHWkcpOIPJy93_rASzFuX-EB0mhGhoUeRbgGcFnS5L0i8Jf1TkARgqkviDNnXaEb5RdyAw";
        Assert.assertEquals(expected, jwt);
    }
    
    @Test
    public void testTokenCreationEidas() throws Exception
    {
        String jwt = tokenCreator.getEidasAuthenticationToken("pid","method","rp","session_id","request_id",new Date(1492775862));
        String expected = "eyJhbGciOiJSUzI1NiIsImtpZCI6IlN1b21pLmZpLXR1bm5pc3R1cyJ9.eyJyZXFfaWQiOiJyZXF1ZXN0X2lkIiwiYXV0aF9tZXRob2QiOiJtZXRob2QiLCJpc3MiOiJTdW9taS5maS10dW5uaXN0dXMiLCJwaWQiOiJwaWQiLCJleHAiOjE1NTI3NzUsImlhdCI6MTQ5Mjc3NSwicnAiOiJycCIsInNmaV9pZCI6InNlc3Npb25faWQifQ.DMI7bY9uVGzgFRjkLdO4_ZrV9Mr7UjVF8BrJvsK83_3Thp42M7SRfoyn2xRHdjiAF86UB97KWAZhGc36TsbAgZ5fXGaAQCsDXmgpclDp2fp-1g8Z7z96L-BrxjsthRcAytqmoRDgJw-2Texp0JTutgXGXfjj2XqdN1I9CGOTobcABa6e-_9Ba8DwrKIiYGIeGSGhIZt-AvvVMs9ILn8IZvLQdATwNNNWTgXJH8rVo-zNmFOoyc0-eccjyPCqqs4Bb6cVpQFfmV2Tbzg3I-9MVwSEZdCgcUVhHgtC1YTnH743U2yaTT3vxf-rF5I0yRtpoikkDyxYRklue2UZU-o7iw";
        Assert.assertEquals(expected, jwt);
    }
    @Test
    public void testTokenCreationForeign() throws Exception
    {
        String jwt = tokenCreator.getForeignPersonAuthenticationToken("fpid","method","rp","session_id","request_id",new Date(1492775862));
        String expected = "eyJhbGciOiJSUzI1NiIsImtpZCI6IlN1b21pLmZpLXR1bm5pc3R1cyJ9.eyJyZXFfaWQiOiJyZXF1ZXN0X2lkIiwiYXV0aF9tZXRob2QiOiJtZXRob2QiLCJmcGlkIjoiZnBpZCIsImlzcyI6IlN1b21pLmZpLXR1bm5pc3R1cyIsImV4cCI6MTU1Mjc3NSwiaWF0IjoxNDkyNzc1LCJycCI6InJwIiwic2ZpX2lkIjoic2Vzc2lvbl9pZCJ9.Hs7X_FrFU2mpfAe_9pa0RmnsxB8KzfZIyrutELr0tf3DEM53_RHE0ZPL7dN4Wse-pIkZ6K37soF8ir2FUy5jtrKBH0vk9VAdn_ypYXetjm0NpBwuAMcGKZrqS84Rynk8dJ8VMhSAOONMy4RvP3duTVqmkcc6aKgVyQVuVwc_G3PuXaAfVFqJsKqZfvPSIFjVgORUTSxu4s9_iJNR5sWav_e0a192VBnnURZuyezOgAaYa81EBtXMcVNq-mmykm9ko_rcyCxVPvBtKYcveo7MaQxPp-Tf2wUgiVRi5_G2iaccFSqug4i_G7_hOsw7Jw0YH-EehrUugmTMYAYcBWmkVg";
        Assert.assertEquals(expected, jwt);
    }

    @Test
    public void testTokenExpiration() throws Exception {
        Date issuedAt = new Date();
        int expirationTime = tokenCreator.getExpirationTime();
        String jwt = tokenCreator.getAuthenticationToken("hetu","method","rp","session_id","request_id", issuedAt);

        Date expires = JWT.decode(jwt).getExpiresAt();
        Assert.assertTrue(expires.after(issuedAt));

        Date notExpired = changeDate(issuedAt, expirationTime - 1);
        Assert.assertTrue(expires.after(notExpired));

        Date expired = changeDate(issuedAt, expirationTime + 1);
        Assert.assertFalse(expires.after(expired));
    }

    private Date changeDate(Date iat, int addMinutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(iat);
        cal.add(Calendar.MINUTE, addMinutes);
        return cal.getTime();
    }
}