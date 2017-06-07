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
import java.util.Date;

public class TokenCreator {

    private static final Logger logger = LoggerFactory.getLogger(TokenCreator.class);

    private final Algorithm algorithm;

    public TokenCreator(Algorithm algorithm) { this.algorithm = algorithm; }

    public String getAuthenticationToken(String hetu, String method, String rp, String sfi_id, String req_id, Date iat) throws TokenCreatorException {

        String token = null;
        try {
            token = JWT.create()
                    .withIssuer("Suomi.fi-tunnistus")
                    .withIssuedAt(iat)
                    .withClaim("auth_method", method)
                    .withClaim("hetu", hetu)
                    .withClaim("rp", rp)
                    .withClaim("sfi_id", sfi_id)
                    .withClaim("req_id", req_id)
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            logger.error("Unable to create JWT: " + e.getMessage());
            throw new TokenCreatorException("JWT creation failed: "+e.getMessage());
        }
        return token;
    }
}
