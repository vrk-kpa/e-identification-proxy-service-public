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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

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
        String expected = "eyJhbGciOiJSUzI1NiJ9.eyJyZXFfaWQiOiJyZXF1ZXN0X2lkIiwiYXV0aF9tZXRob2QiOiJtZXRob2QiLCJpc3MiOiJTdW9taS5maS10dW5uaXN0dXMiLCJpYXQiOjE0OTI3NzUsImhldHUiOiJoZXR1IiwicnAiOiJycCIsInNmaV9pZCI6InNlc3Npb25faWQifQ.XP7Mp0PfssD8K-YNY_jUas65IFHuWjOp8lRMl7iTeS1tCUx1NrhD3Zkskct2nuxvJXyMltqX7R2gs3dPPlTKLs2gUYCWvTkUarYX6uqhit5VLQWEnaU9qz6b5wZolPZzW-V2dGimXbodVzEQyzzm3bLuh68vulmaFJZHXrpYnN56bbV2LNRHFvJg9WUXYLXrgtQEAS0bg3vqfZp6eK2YBenegSNGhroHz1WmEoASE83fmpgt_tizK09NlE0xaQk1edhAW3WX21SN7NJDqmOvAgYFgjbC7K447s04UnTq56-Fzcdo9uLUjcJ15QsI6_fk0p4OoFB1_w_M5as4LiLjbg";
        Assert.assertEquals(jwt, expected);
    }
}