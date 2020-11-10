/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: Jul 16, 2015                                                      
 *
 *******************************************************************************/
package org.oscm.app.business;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.business.exceptions.ServiceInstanceInProcessingException;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.provisioning.data.BaseResult;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ProvisioningResults.class})
public class ProvisioningResultsTest {

    private ProvisioningResults provResult;
    private Exception exception;
    private ServiceInstance serviceInstance;
    Class<BaseResult> clazz = BaseResult.class;

    @Before
    public void setUp() {
        provResult = PowerMockito.spy(new ProvisioningResults());
        exception = mock(Exception.class);
        serviceInstance = mock(ServiceInstance.class);
    }

    @Test
    public void testGetOKResult() {

        BaseResult result = provResult.getOKResult(clazz);

        assertEquals("Ok", result.getDesc());
        assertEquals(0, result.getRc());
    }

    @Test
    public void testNewOKBaseResult() {

        BaseResult result = provResult.newOkBaseResult();

        assertEquals("Ok", result.getDesc());
        assertEquals(0, result.getRc());
    }

    @Test
    public void testGetSuccessfulResult() {

        String successMsg = "successMsg";

        BaseResult result = provResult.getSuccesfulResult(clazz, successMsg);

        assertEquals(successMsg, result.getDesc());
        assertEquals(0, result.getRc());
    }

    @Test
    public void testErrorResult() {
        String errMessage = "Test error message";
        when(exception.getMessage()).thenReturn(errMessage);

        BaseResult result = provResult.getErrorResult(clazz, exception, "en", serviceInstance, "Test instance");

        assertEquals(1, result.getRc());
        assertEquals(errMessage, result.getDesc());
    }

    @Test
    public void testErrorResultInstanceNotFound() {

        BaseResult result = provResult.getErrorResult(clazz, new ServiceInstanceNotFoundException(""), "en", serviceInstance, "Test instance");

        assertEquals(1, result.getRc());
        assertTrue(result.getDesc().contains("The instance with ID"));
    }

    @Test
    public void testErrorResultInstanceInProcessing() {

        BaseResult result = provResult.getErrorResult(clazz, new ServiceInstanceInProcessingException(""), "en", serviceInstance, "Test instance");

        assertEquals(1, result.getRc());
        assertTrue(result.getDesc().contains("This operation is currently not available"));
    }

    @Test
    public void testErrorResultAPPlatformException() {
        String errMessage = "Test exception APPlatformException message";

        BaseResult result = provResult.getErrorResult(clazz, new APPlatformException(errMessage), "en", serviceInstance, "Test instance");

        assertEquals(1, result.getRc());
        assertEquals(errMessage, result.getDesc());
    }

    @Test
    public void testGetInstance() throws Exception {
        String instanceId = "Test instance";

        ServiceInstance result = Whitebox.invokeMethod(provResult, "getInstance", serviceInstance, instanceId);

        assertTrue(result.getClass().getSimpleName().contains("ServiceInstance"));
        verify(serviceInstance, never()).setInstanceId(anyString());
    }

    @Test
    public void testGetInstanceWithNull() throws Exception {
        String instanceId = "Test instance";
        ServiceInstance instance = null;

        ServiceInstance result = Whitebox.invokeMethod(provResult, "getInstance", instance, instanceId);

        assertEquals(instanceId, result.getInstanceId());
    }

    @Test
    public void testIsError() {
        BaseResult baseResult = new BaseResult();
        baseResult.setRc(0);

        assertFalse(provResult.isError(baseResult));
    }
}
