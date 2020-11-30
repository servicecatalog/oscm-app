/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 20.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business.balancer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.business.VMwareDatacenterInventory;
import org.oscm.app.vmware.business.model.VMwareStorage;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({DynamicEquipartitionStorageBalancer.class})
public class DynamicEquipartitionStorageBalancerTest {

  @InjectMocks
  private DynamicEquipartitionStorageBalancer desBalancer;
  private VMPropertyHandler properties;
  private VMwareDatacenterInventory inventory;
  private VMwareStorage vMwareStorage;
  private List<VMwareStorage> hosts;

  @Before
  public void setUp() {
    desBalancer = PowerMockito.spy(new DynamicEquipartitionStorageBalancer());
    properties = mock(VMPropertyHandler.class);
    inventory = mock(VMwareDatacenterInventory.class);
    vMwareStorage = mock(VMwareStorage.class);
    MockitoAnnotations.initMocks(this);

    hosts = new ArrayList<>();
    hosts.add(vMwareStorage);
  }

  @Test
  public void testNext() throws APPlatformException {
    //given
    when(properties.getServiceSetting(anyString())).thenReturn("TargetHost");
    when(inventory.getStorageByHost(anyString())).thenReturn(hosts);
    when(vMwareStorage.getFree()).thenReturn(1.0);
    //when
    VMwareStorage result = desBalancer.next(properties);
    //then
    verify(vMwareStorage, times(2)).getFree();
    assertEquals(vMwareStorage, result);
  }

  @Test(expected = APPlatformException.class)
  public void testNextNullTargetHost() throws APPlatformException {
    //when
    desBalancer.next(properties);
  }

  @Test(expected = APPlatformException.class)
  public void testNextStorageFreeLessThanMaxSpace() throws APPlatformException {
    //given
    when(properties.getServiceSetting(anyString())).thenReturn("TargetHost");
    when(inventory.getStorageByHost(anyString())).thenReturn(hosts);
    //when
    desBalancer.next(properties);
  }
}
