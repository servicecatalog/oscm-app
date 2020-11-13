/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2018
 *                                                                              
 *  Creation Date: 29.08.2012                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.v2_0.service;

import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.oscm.app.business.ProductProvisioningServiceFactoryBean;
import org.oscm.app.business.exceptions.BadResultException;
import org.oscm.app.domain.InstanceParameter;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.provisioning.intf.ProvisioningService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.xml.ws.Service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ProductProvisioningServiceFactoryBean.class, Service.class})
public class ProductProvisioningServiceFactoryBeanTest {

    private ProductProvisioningServiceFactoryBean factory;
    private InstanceParameter PUBLIC_IP;
    private InstanceParameter WSDL;
    private InstanceParameter PROTOCOL;
    private InstanceParameter PORT;
    private InstanceParameter USER;
    private InstanceParameter USER_PWD;
    private Service service;

    @Before
    public void setup() {

        factory = new ProductProvisioningServiceFactoryBean();

        PowerMockito.mockStatic(Service.class);
        service = mock(Service.class);

        PUBLIC_IP = new InstanceParameter();
        PUBLIC_IP.setParameterKey(InstanceParameter.PUBLIC_IP);
        PUBLIC_IP.setParameterValue("127.0.0.1");
        WSDL = new InstanceParameter();
        WSDL.setParameterKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_WSDL);
        WSDL.setParameterValue("wsdl");
        PROTOCOL = new InstanceParameter();
        PROTOCOL.setParameterKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_PROTOCOL);
        PROTOCOL.setParameterValue("http");
        PORT = new InstanceParameter();
        PORT.setParameterKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_PORT);
        PORT.setParameterValue("1234");
        USER = new InstanceParameter();
        USER.setParameterKey(InstanceParameter.SERVICE_USER);
        USER.setParameterValue("mustermann");
        USER_PWD = new InstanceParameter();
        USER_PWD.setParameterKey(InstanceParameter.SERVICE_USER_PWD);
        USER_PWD.setParameterValue("secret");
    }

    @Test
    public void test_ReturnResult() throws Throwable {
        ServiceInstance instance = new ServiceInstance();
        ArrayList<InstanceParameter> params = new ArrayList<InstanceParameter>();
        params.add(PUBLIC_IP);
        params.add(WSDL);
        params.add(PROTOCOL);
        params.add(PORT);
        params.add(USER);
        params.add(USER_PWD);
        instance.setInstanceParameters(params);
        when(Service.create((URL) any(), any())).thenReturn(service);

        factory.getInstance(instance);

        verify(service, times(1)).getPort(ProvisioningService.class);
    }

    @Test(expected = BadResultException.class)
    public void test_null() throws Exception {
        getInstance(null);
    }

    @Test(expected = BadResultException.class)
    public void test_noIp() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceParameters(new ArrayList<InstanceParameter>());
        getInstance(instance);
    }

    @Test(expected = BadResultException.class)
    public void test_noRelativeWSDL() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        ArrayList<InstanceParameter> params = new ArrayList<InstanceParameter>();
        params.add(PUBLIC_IP);
        instance.setInstanceParameters(params);
        getInstance(instance);
    }

    @Test(expected = BadResultException.class)
    public void test_noProtocol() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        ArrayList<InstanceParameter> params = new ArrayList<InstanceParameter>();
        params.add(PUBLIC_IP);
        params.add(WSDL);
        instance.setInstanceParameters(params);
        getInstance(instance);
    }

    @Test(expected = BadResultException.class)
    public void test_noPort() throws Exception {
        ServiceInstance instance = new ServiceInstance();
        ArrayList<InstanceParameter> params = new ArrayList<InstanceParameter>();
        params.add(PUBLIC_IP);
        params.add(WSDL);
        params.add(PROTOCOL);
        instance.setInstanceParameters(params);
        getInstance(instance);
    }

    private ProvisioningService getInstance(final ServiceInstance instance)
            throws Exception {
        return factory.getInstance(instance);
    }
}
