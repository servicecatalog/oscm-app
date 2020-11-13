/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: 10.11.2020
 *
 *******************************************************************************/
package org.oscm.app.v2_0.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.persistence.EntityManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({APPConcurrencyServiceBean.class})
public class AppConcurrencyServiceBeanTest {

  @InjectMocks
  private APPConcurrencyServiceBean concurrencyService;

  private ServiceInstanceDAO instanceDAO;
  private ServiceInstance serviceInstance;
  private EntityManager entityManager;

  @Before
  public void setup() {
    concurrencyService = new APPConcurrencyServiceBean();
    instanceDAO = mock(ServiceInstanceDAO.class);
    serviceInstance = mock(ServiceInstance.class);
    entityManager = mock(EntityManager.class);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testLockServiceInstanceReturnFalse() throws Exception {
    when(instanceDAO.getLockedInstanceForController(anyString())).thenReturn(serviceInstance);
    when(serviceInstance.getInstanceId()).thenReturn("WrongInstance");

    assertFalse(concurrencyService.lockServiceInstance("ControllerId", "InstanceId"));
  }

  @Test
  public void testLockServiceInstanceReturnTrue() throws Exception {
    when(instanceDAO.getLockedInstanceForController(anyString())).thenReturn(serviceInstance);
    when(serviceInstance.getInstanceId()).thenReturn("InstanceId");

    assertTrue(concurrencyService.lockServiceInstance("ControllerId", "InstanceId"));
  }

  @Test
  public void testLockServiceInstanceLockService() throws Exception {
    when(instanceDAO.getLockedInstanceForController(anyString())).thenReturn(null);
    when(instanceDAO.getInstanceById(anyString(), anyString())).thenReturn(serviceInstance);

    assertTrue(concurrencyService.lockServiceInstance("ControllerId", "InstanceId"));
  }

  @Test(expected = APPlatformException.class)
  public void testLockServiceInstanceNotFound() throws Exception {
    when(instanceDAO.getLockedInstanceForController(anyString())).thenReturn(null);
    when(instanceDAO.getInstanceById(anyString(), anyString())).thenThrow(new ServiceInstanceNotFoundException("Instance not found"));

    concurrencyService.lockServiceInstance("ControllerId", "InstanceId");
  }

  @Test
  public void testUnlockServiceInstance() throws Exception {
    when(instanceDAO.getInstanceById(anyString(), anyString())).thenReturn(serviceInstance);
    when(serviceInstance.isLocked()).thenReturn(true);

    concurrencyService.unlockServiceInstance("ControllerId", "InstanceId");

    verify(entityManager, times(1)).flush();
  }

  @Test(expected = APPlatformException.class)
  public void testUnlockServiceInstanceNotFound() throws Exception {
    when(instanceDAO.getInstanceById(anyString(), anyString())).thenThrow(new ServiceInstanceNotFoundException("Instance not found"));

    concurrencyService.unlockServiceInstance("ControllerId", "InstanceId");
  }
}
