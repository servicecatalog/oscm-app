/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 16.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.approval.data.State;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({Dispatcher.class})
public class DispatcherTest {

  private Dispatcher dispatcher;
  private APPlatformService platformService;
  private PropertyHandler propertyHandler;
  private InstanceStatus instanceStatus;
  private ProvisioningSettings settings;

  @Before
  public void setup() {
    platformService = mock(APPlatformService.class);
    propertyHandler = mock(PropertyHandler.class);
    instanceStatus = mock(InstanceStatus.class);
    settings = mock(ProvisioningSettings.class);

    dispatcher = new Dispatcher(platformService, "instanceId", propertyHandler);
  }

  @Test
  public void testGetControllerId() throws Exception {

    when(propertyHandler.getState()).thenReturn(State.CREATING);
    PowerMockito.whenNew(InstanceStatus.class).withNoArguments().thenReturn(instanceStatus);
    when(propertyHandler.getSettings()).thenReturn(settings);
    when(instanceStatus.isReady()).thenReturn(true);

    InstanceStatus result = dispatcher.dispatch();

    verify(instanceStatus, times(1)).setAccessInfo("Access information for instance instanceId");
    assertEquals(instanceStatus, result);
  }
}
