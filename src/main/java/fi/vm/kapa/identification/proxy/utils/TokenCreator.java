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
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import fi.vm.kapa.identification.proxy.exception.TokenCreatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TokenCreator {

    public static final String KID_HEADERCLAIM_KEY = "kid";
    public static final String AUTHMETHOD_CLAIM_KEY = "auth_method";
    public static final String HETU_CLAIM_KEY = "hetu";
    public static final String RP_CLAIM_KEY = "rp";
    public static final String SFI_ID_CLAIM_KEY = "sfi_id";
    public static final String REQ_ID_CLAIM_KEY = "req_id";
    public static final String PID_CLAIM_KEY = "pid";

    private static final Logger logger = LoggerFactory.getLogger(TokenCreator.class);

    private final Algorithm algorithm;
    private final String issuer;
    private final Map<String, Object> headers;
    private final int expirationTime;

    public TokenCreator(Algorithm algorithm, final String issuer, String tokenHeaderKid, int expirationTime) {
        this.algorithm = algorithm;
        this.issuer = issuer;
        Map<String, Object> original = new HashMap<>();
        original.put(KID_HEADERCLAIM_KEY, tokenHeaderKid);
        headers = Collections.unmodifiableMap(original);
        this.expirationTime = expirationTime;
    }

    public String getAuthenticationToken(String hetu, String method, String rp, String sfi_id, String req_id, Date iat) throws TokenCreatorException {

        try {
            return  JWT.create()
                    .withIssuer(issuer)
                    .withHeader(headers)
                    .withIssuedAt(iat)
                    .withExpiresAt(getExpiresAt(iat))
                    .withClaim(AUTHMETHOD_CLAIM_KEY, method)
                    .withClaim(HETU_CLAIM_KEY, hetu)
                    .withClaim(RP_CLAIM_KEY, rp)
                    .withClaim(SFI_ID_CLAIM_KEY, sfi_id)
                    .withClaim(REQ_ID_CLAIM_KEY, req_id)
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            logger.error("Unable to create JWT: " + e.getMessage());
            throw new TokenCreatorException("JWT creation failed: " + e.getMessage());
        }
    }

    public String getEidasAuthenticationToken(String pid, String method, String rp, String sfi_id, String req_id, Date iat) throws TokenCreatorException {
        try {
            return  JWT.create()
                    .withIssuer(issuer)
                    .withHeader(headers)
                    .withIssuedAt(iat)
                    .withExpiresAt(getExpiresAt(iat))
                    .withClaim(AUTHMETHOD_CLAIM_KEY, method)
                    .withClaim(PID_CLAIM_KEY, pid)
                    .withClaim(RP_CLAIM_KEY, rp)
                    .withClaim(SFI_ID_CLAIM_KEY, sfi_id)
                    .withClaim(REQ_ID_CLAIM_KEY, req_id)
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            logger.error("Unable to create JWT: " + e.getMessage());
            throw new TokenCreatorException("JWT creation failed: " + e.getMessage());
        }
    }


    private Date getExpiresAt(Date iat) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(iat);
        cal.add(Calendar.MINUTE, expirationTime);
        return cal.getTime();
    }

    int getExpirationTime() {
        return expirationTime;
    }
}
