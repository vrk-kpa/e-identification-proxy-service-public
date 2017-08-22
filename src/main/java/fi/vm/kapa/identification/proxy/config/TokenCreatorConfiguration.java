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
package fi.vm.kapa.identification.proxy.config;

import com.auth0.jwt.algorithms.Algorithm;
import fi.vm.kapa.identification.proxy.exception.TokenCreatorException;
import fi.vm.kapa.identification.proxy.utils.TokenCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;

@Configuration
public class TokenCreatorConfiguration {

    @Value("${token.keystore}")
    private String tokenKeystoreFilename;

    @Value("${token.keystore.alias}")
    private String tokenKeystoreAlias;

    @Value("${token.keystore.password}")
    private String tokenKeystorePass;

    @Value("${token.keystore.keypassword}")
    private String tokenKeystoreKeyPass;

    @Value("${token.issuer}")
    private String tokenIssuer;

    private static final Logger logger = LoggerFactory.getLogger(TokenCreatorConfiguration.class);

    @Bean(name = "tokenCreator")
    TokenCreator provideTokenCreator() throws TokenCreatorException {

        KeyStore tokenKeyStore;

        try (FileInputStream is = new FileInputStream(tokenKeystoreFilename)) { // try-with-resources; AutoCloseable
            tokenKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            tokenKeyStore.load(is, tokenKeystorePass.toCharArray());
        } catch (IOException e) {
            throw new TokenCreatorException("KeyStore access problem: " + e.getMessage());
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw new TokenCreatorException("KeyStore instantiation problem: " + e.getMessage());
        }

        try {
            Key key = tokenKeyStore.getKey(tokenKeystoreAlias, tokenKeystoreKeyPass.toCharArray());
            Algorithm algorithm = Algorithm.RSA256((RSAPrivateKey) key);
            return new TokenCreator(algorithm, tokenIssuer);
        } catch (KeyStoreException e) {
            throw new TokenCreatorException("KeyStore problem: ", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TokenCreatorException("KeyStore algorithm problem: ", e);
        } catch (UnrecoverableKeyException e) {
            throw new TokenCreatorException("KeyStore key problem: ", e);
        }
    }

}
