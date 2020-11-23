/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 13.10.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.v2_0.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.ProvisioningStatus;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.operation.data.OperationParameter;
import org.oscm.operation.data.OperationResult;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*", "javax.net.ssl.*"})
@PrepareForTest({AsynchronousOperationProxy.class, ServiceInstanceDAO.class, OperationServiceBean.class, APPTimerServiceBean.class, ProvisioningStatus.class})
public class AsynchronousOperationProxyTest {

  @InjectMocks
  private AsynchronousOperationProxy asp;

  private OperationServiceBean operationBean;
  private ServiceInstanceDAO instanceDAO;
  private APPTimerServiceBean timerService;
  private Properties properties;
  private OperationResult operationResult;
  private ServiceInstance serviceInstance;
  private ProvisioningStatus provisioningStatus;

  @Before
  public void setup() {
    asp = new AsynchronousOperationProxy();
    operationBean = mock(OperationServiceBean.class);
    instanceDAO = mock(ServiceInstanceDAO.class);
    timerService = mock(APPTimerServiceBean.class);
    properties = mock(Properties.class);
    operationResult = mock(OperationResult.class);
    serviceInstance = mock(ServiceInstance.class);
    provisioningStatus = mock(ProvisioningStatus.class);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testExecuteServiceOperation() throws ServiceInstanceNotFoundException {

    List<OperationParameter> parameters = new ArrayList<>();
    when(operationBean.createProperties(anyString(), anyString(), anyList())).thenReturn(properties);
    when(operationBean.execute(anyString(), anyString(), anyString(), anyString(), any(), anyLong())).thenReturn(operationResult);
    when(instanceDAO.getInstanceById(anyString())).thenReturn(serviceInstance);
    when(serviceInstance.getProvisioningStatus()).thenReturn(provisioningStatus);
    when(provisioningStatus.isWaitingForOperation()).thenReturn(true);

    asp.executeServiceOperation("userId", "instanceId", "transactionId", "operationId", parameters);

    verify(timerService, times(1)).initTimers();
  }

  @Test
  public void testGetParameterValues() {

    when(operationBean.getParameterValues(anyString(), anyString(), anyString())).thenReturn(new ArrayList<>());

    asp.getParameterValues("userId", "instanceId", "operationId");

    verify(operationBean, times(1)).getParameterValues("userId", "instanceId", "operationId");
  }
}
