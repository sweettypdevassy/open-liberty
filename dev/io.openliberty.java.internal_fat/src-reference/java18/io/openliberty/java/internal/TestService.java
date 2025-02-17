/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.java.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
@ApplicationScoped
public class TestService {

    private StringWriter sw = new StringWriter();

    @GET
    public String test() {
        try {
            log(">>> ENTER");
            doTest();
            log("<<< EXIT SUCCESSFUL");
        } catch (Exception e) {
            e.printStackTrace(System.out);
            e.printStackTrace(new PrintWriter(sw));
            log("<<< EXIT FAILED");
        }
        String result = sw.toString();
        sw = new StringWriter();
        return result;
    }

    private void doTest() throws Exception {
        log("Begining Java 17 testing");

        SwitchPatternMatching(RandomPick());

        log("Goodbye testing");
    }

    // JEP 356: Enhanced Pseudo-Random Number Generators - https://openjdk.java.net/jeps/356
    public Object RandomPick() {
        RandomGeneratorFactory<RandomGenerator> rgf = RandomGeneratorFactory.of("L64X128MixRandom");
        RandomGenerator rng = rgf.create();
        int rnd = rng.nextInt(6);
        if (rnd == 0) {
            return RandomString(rng);
        } else if (rnd == 1) {
            return rng.nextInt();
        } else if (rnd == 2) {
            return rng.nextLong();
        } else if (rnd == 3) {
            return rng.nextFloat();
        } else if (rnd == 4) {
            return rng.nextDouble();
        } else {
            return rng.nextBoolean();
        }
    }

    public String RandomString(RandomGenerator rng) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
        int len = rng.nextInt(35) + 5;
        IntStream picks = rng.ints(len, 0, characters.length());
        StringBuilder sb = new StringBuilder("(" + len + ") ");

        Iterator<Integer> it = picks.iterator();

        while (it.hasNext()) {
            sb.append(characters.charAt(it.next()));
        }

        return sb.toString();
    }

    // JEP 406: Pattern Matching for switch (Preview) - https://openjdk.java.net/jeps/406
    public void SwitchPatternMatching(Object o) {
        switch (o) {
            case null         -> log("Random value was null");
            case Integer i    -> log("Random value was an integer: " + i);
            case Long l       -> log("Random value was a long: " + l);
            case Float f      -> log("Random value was a float: " + f);
            case Double d     -> log("Random value was a double: " + d);
            case Boolean b    -> log("Random value was a boolean: " + b);
            case String s     -> log("Random value was a String: " + s);
            default           -> log("Forgot to check for this type: " + o.getClass().toString());
        }
    }

    public void log(String msg) {
        System.out.println(msg);
        sw.append(msg);
        sw.append("<br/>");
    }

    /*
     * Extend a class which is introduced in Java 18, to ensure that this class cannot be compiled with anything eariler than Java 18.
     * JEP 418: Internet-Address Resolution SPI https://openjdk.java.net/jeps/418
     */
    public class JDK18TestInetAddressResolverProvider extends java.net.spi.InetAddressResolverProvider {
        public java.net.spi.InetAddressResolver get(java.net.spi.InetAddressResolverProvider.Configuration configuration) {
            return null;
        }

        public String name() {
            return "nothing";
        }
    }
}
