/*******************************************************************************
 * Copyright (c) 2011, 2024 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.service.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;
import test.common.SharedOutputManager;
import test.utils.Utils;

/**
 *
 */
@RunWith(JMock.class)
public class AtomicServiceReferenceTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    Mockery context = new Mockery();

    ComponentContext mockComponentContext = context.mock(ComponentContext.class);
    ServiceReference<String> mockServiceReference1 = ComponentContextMockery.mockService(context, mockComponentContext, "1", null, 1, null);
    ServiceReference<String> mockServiceReference2 = ComponentContextMockery.mockService(context, mockComponentContext, "2", null, 2, null);

    @Test
    public void testAtomicServiceReference() {
        final String m = "testAtomicServiceReference";
        try {
            AtomicServiceReference<String> test = new AtomicServiceReference<String>("string");

            assertNull("A-1 getReference should return null when ref not set: " + test, test.getReference());
            assertNull("A-2 getService should return null when ref not set: " + test, test.getService());

            boolean setResult = test.setReference(mockServiceReference1);
            assertSame("B-1 getReference should return the set reference: " + test, mockServiceReference1, test.getReference());
            assertFalse("B-2 setReference is not replacing a previous value: " + test, setResult);
            assertNull("B-3 getService should return null-- context not set: " + test, test.getService());
            setResult = test.setReference(mockServiceReference2);
            assertTrue("B-4 setReference is not replacing a previous value: " + test, setResult);

            final String service = "Dummy Service.. obviously not invokable";

            context.checking(new Expectations() {
                {
                    one(mockComponentContext).locateService("string", mockServiceReference2);
                    will(returnValue(service));
                }
            });

            test.activate(mockComponentContext);
            assertSame("C-1 getReference should return the set reference: " + test, mockServiceReference2, test.getReference());
            assertSame("C-2 getService should return result of locateService: " + test, service, test.getService());
            assertSame("C-3 getService should return service without another call to locateService: " + test, service, test.getService());

            test.deactivate(mockComponentContext);
            assertSame("D-1 getReference should return the set reference: " + test, mockServiceReference2, test.getReference());
            assertNull("D-2 getService should return null-- context not set: " + test, test.getService());

            context.checking(new Expectations() {
                {
                    one(mockComponentContext).locateService("string", mockServiceReference2);
                    will(returnValue(service));
                }
            });

            test.activate(mockComponentContext);
            assertSame("E-1 getReference should return the set reference: " + test, mockServiceReference2, test.getReference());
            assertSame("E-2 getService should return result of locateService: " + test, service, test.getService());

            setResult = test.unsetReference(mockServiceReference1);
            // should be false because ref2 is higher rank;
            assertFalse("Should not have replaced.", setResult);
            setResult = test.unsetReference(mockServiceReference2);
            assertTrue("Should have replaced.", setResult);
            assertNull("E-3 getReference should return null when ref not set: " + test, test.getReference());
            assertNull("E-4 getService should return null when ref not set: " + test, test.getService());

            test.setReference(mockServiceReference1);
            setResult = test.unsetReference(mockServiceReference2);
            assertFalse("Unset did not match with 'current' reference: " + test, setResult);
            assertTrue("Should have replaced with null", test.unsetReference(mockServiceReference1));

            // order setReference calls should not matter
            assertFalse("Should replace when when current is null.", test.setReference(mockServiceReference2));
            assertFalse("Should not replace with lower rank.", test.setReference(mockServiceReference1));
            assertEquals("Wrong reference.", mockServiceReference2, test.getReference());
            assertTrue("Should have replaced lower rank.", test.unsetReference(mockServiceReference2));
            assertEquals("Wrong reference.", mockServiceReference1, test.getReference());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.AtomicServiceReference#getServiceWithException()}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionWithNullContext() {
        AtomicServiceReference<String> aRef = new AtomicServiceReference<String>("string");
        aRef.setReference(mockServiceReference1);
        aRef.getServiceWithException();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.AtomicServiceReference#getServiceWithException()}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionWithNullServiceRef() {
        AtomicServiceReference<String> aRef = new AtomicServiceReference<String>("string");
        aRef.activate(mockComponentContext);
        aRef.getServiceWithException();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.AtomicServiceReference#getServiceWithException()}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionCantFindLocatedService() {
        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("string", mockServiceReference1);
                will(returnValue(null));
            }
        });
        AtomicServiceReference<String> aRef = new AtomicServiceReference<String>("string");
        aRef.setReference(mockServiceReference1);
        aRef.activate(mockComponentContext);
        aRef.getServiceWithException();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.AtomicServiceReference#getServiceWithException()}.
     */
    @Test
    public void getServiceWithExceptionWithValidService() {
        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("string", mockServiceReference1);
            }
        });

        AtomicServiceReference<String> aRef = new AtomicServiceReference<String>("string");
        aRef.setReference(mockServiceReference1);
        aRef.activate(mockComponentContext);
        assertNotNull(aRef.getServiceWithException());
    }

    @Test
    public void getServiceReset() {
        final String service1 = "service 1";
        final String service2 = "service 2";

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("string", mockServiceReference1);
                will(returnValue(service1));

                one(mockComponentContext).locateService("string", mockServiceReference2);
                will(returnValue(service2));
            }
        });

        AtomicServiceReference<String> aRef = new AtomicServiceReference<String>("string");
        aRef.activate(mockComponentContext);

        aRef.setReference(mockServiceReference1);
        assertEquals(service1, aRef.getService());

        // Do not replace/remove located object if the same service reference is set
        // a second time..
        aRef.setReference(mockServiceReference1);
        assertEquals(service1, aRef.getService());

        aRef.setReference(mockServiceReference2);
        assertEquals(service2, aRef.getService());
    }
}
