/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: Nov 9, 2020
 *
 *******************************************************************************/
package org.oscm.app.business;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.business.exceptions.ServiceInstanceNotFoundException;
import org.oscm.app.dao.ServiceInstanceDAO;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.ServiceUser;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.intf.APPlatformController;
import org.oscm.app.v2_0.service.APPConfigurationServiceBean;
import org.oscm.app.v2_0.service.APPTimerServiceBean;
import org.oscm.provisioning.data.BaseResult;
import org.oscm.provisioning.data.User;
import org.oscm.provisioning.intf.ProvisioningService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({AsynchronousProvisioningProxyImpl.class, APPlatformControllerFactory.class, UserMapper.class})
public class AsynchronousProvisioningProxyImplTest {

  @InjectMocks
  private AsynchronousProvisioningProxyImpl provisioningProxyImpl;

  private ServiceInstance instance;
  private User user;
  private APPlatformController controller;
  private ServiceUser serviceUser;
  private ProvisioningSettings settings;
  private APPConfigurationServiceBean configService;
  private InstanceStatus status;
  private ProvisioningService provisioning;
  private BaseResult baseResult;
  private ProvisioningResults provResultHelper;
  private ProductProvisioningServiceFactoryBean provisioningFactory;
  private ServiceInstanceDAO instanceDAO;

  // It is necessary due to Injection
  private APPTimerServiceBean timerService;
  private Logger logger;

  @Before
  public void setUp() {
    provisioningProxyImpl = new AsynchronousProvisioningProxyImpl();
    PowerMockito.mockStatic(APPlatformControllerFactory.class);
    PowerMockito.mockStatic(UserMapper.class);
    configService = mock(APPConfigurationServiceBean.class);
    instance = mock(ServiceInstance.class);
    user = mock(User.class);
    controller = mock(APPlatformController.class);
    serviceUser = mock(ServiceUser.class);
    settings = mock(ProvisioningSettings.class);
    status = mock(InstanceStatus.class);
    timerService = mock(APPTimerServiceBean.class);
    provResultHelper = mock(ProvisioningResults.class);
    provisioning = mock(ProvisioningService.class);
    provisioningFactory = mock(ProductProvisioningServiceFactoryBean.class);
    baseResult = mock(BaseResult.class);
    instanceDAO = mock(ServiceInstanceDAO.class);
    logger = mock(Logger.class);

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetInstance() throws Exception {
    PowerMockito.whenNew(APPConfigurationServiceBean.class).withNoArguments().thenReturn(configService);
    when(instance.getSubscriptionId()).thenReturn("SubscriptionID");
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(UserMapper.toServiceUser(user)).thenReturn(serviceUser);
    when(configService.getProvisioningSettings(instance, serviceUser)).thenReturn(settings);
    when(controller.deleteInstance(anyString(), any())).thenReturn(status);

    provisioningProxyImpl.deleteInstance(instance, user);

    verify(instance, never()).getOrganizationId();
    verify(instance, times(1)).setInstanceParameters((HashMap<String, Setting>) anyMap());
  }

  @Test
  public void testGetInstanceStatusIsInstance() throws Exception {
    PowerMockito.whenNew(APPConfigurationServiceBean.class).withNoArguments().thenReturn(configService);
    when(instance.getSubscriptionId()).thenReturn("SubscriptionID");
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(UserMapper.toServiceUser(user)).thenReturn(serviceUser);
    when(configService.getProvisioningSettings(instance, serviceUser)).thenReturn(settings);
    when(controller.deleteInstance(anyString(), any())).thenReturn(status);
    when(status.isInstanceProvisioningRequested()).thenReturn(true);
    when(provisioningFactory.getInstance(instance)).thenReturn(provisioning);
    when(provisioning.deleteInstance(anyString(), anyString(), anyString(), any())).thenReturn(baseResult);

    provisioningProxyImpl.deleteInstance(instance, user);

    verify(instance, times(1)).getOrganizationId();
    verify(instance, times(1)).setInstanceParameters((HashMap<String, Setting>) anyMap());
  }

  @Test
  public void testGetInstanceResultIsError() throws Exception {
    PowerMockito.whenNew(APPConfigurationServiceBean.class).withNoArguments().thenReturn(configService);
    when(instance.getSubscriptionId()).thenReturn("SubscriptionID");
    when(APPlatformControllerFactory.getInstance(anyString())).thenReturn(controller);
    when(UserMapper.toServiceUser(user)).thenReturn(serviceUser);
    when(configService.getProvisioningSettings(instance, serviceUser)).thenReturn(settings);
    when(controller.deleteInstance(anyString(), any())).thenReturn(status);
    when(status.isInstanceProvisioningRequested()).thenReturn(true);
    when(provisioningFactory.getInstance(instance)).thenReturn(provisioning);
    when(provisioning.deleteInstance(anyString(), anyString(), anyString(), any())).thenReturn(baseResult);
    when(provResultHelper.isError(baseResult)).thenReturn(true);

    provisioningProxyImpl.deleteInstance(instance, user);

    verify(instance, times(1)).getOrganizationId();
    verify(instance, never()).setInstanceParameters((HashMap<String, Setting>) anyMap());
  }

  @Test
  public void testDeleteInstanceServiceNotFound() throws Exception {

    when(instanceDAO.getInstance(anyString(), anyString(), anyString())).thenThrow(new ServiceInstanceNotFoundException("Instamce not found"));

    provisioningProxyImpl.deleteInstance(anyString(), anyString(), anyString(), user);

    verify(provResultHelper, times(1)).getOKResult(any());
    verify(provResultHelper, never()).getErrorResult(any(), any(), anyString(), any(), anyString());
  }

  @Test
  public void testDeleteInstanceIsDeleted() throws Exception {

    when(instanceDAO.getInstance(anyString(), anyString(), anyString())).thenReturn(instance);
    when(instance.isDeleted()).thenReturn(true);

    provisioningProxyImpl.deleteInstance(anyString(), anyString(), anyString(), user);

    verify(provResultHelper, times(1)).getOKResult(any());
    verify(provResultHelper, never()).getErrorResult(any(), any(), anyString(), any(), anyString());
  }

  @Test
  public void testDeleteInstanceCatchException() throws Exception {

    when(instanceDAO.getInstance(anyString(), anyString(), anyString())).thenReturn(instance);

    provisioningProxyImpl.deleteInstance(anyString(), anyString(), anyString(), user);

    verify(provResultHelper, never()).getOKResult(any());
    verify(provResultHelper, times(1)).getErrorResult(any(), any(), anyString(), any(), anyString());
  }
}
