/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 14 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.RuntimeFaultFaultMsg;

/** @author worf */
@RunWith(PowerMockRunner.class)
@PrepareForTest(VMClientPool.class)
public class VMwareProcessorTest {

  @Mock VMPropertyHandler ph;
  @Mock VMwareClient client;
  @Mock VM vm;
  @Spy VMwareProcessor vmwareProcessor = new VMwareProcessor();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn("vcenter").when(ph).getServiceSetting(VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
    doReturn(client).when(vmwareProcessor).getVMWareClient(anyString());
    doReturn(vm).when(vmwareProcessor).createVM(anyString(), any());
    doReturn("").when(ph).getAccessInfo();
    doNothing().when(vmwareProcessor).returnVMWareClient(anyString(), any());
    doReturn("name").when(ph).getInstanceName();
  }

  @Test
  public void getServersDetails() {

    // given

    // when
    List<Server> server = vmwareProcessor.getServersDetails(ph);
    // then

    assertEquals("name", server.get(0).getId());
  }

  @Test
  public void getVmAccessUrl() throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {

    // given
    doReturn("URL").when(vm).createVmUrl(ph);
    // when
    String result = vmwareProcessor.getVmAccessUrl(ph);
    // then
    assertEquals("URL", result);
  }
}
