/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 13 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.oscm.app.vmware.business.model.DistributedVirtualSwitch;
import org.oscm.app.vmware.business.model.Portgroup;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.vmware.vim25.DistributedVirtualSwitchPortConnection;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceBackingInfo;
import com.vmware.vim25.VirtualEthernetCard;
import com.vmware.vim25.VirtualEthernetCardDistributedVirtualPortBackingInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRuntimeInfo;

/** @author worf */
@RunWith(PowerMockRunner.class)
@PrepareForTest(NetworkManager.class)
public class NetworkManagerTest {

  @Mock VMwareClient vmw;
  @Mock ManagedObjectReference vmwInstance;
  @Mock ManagedObjectReference mor;
  @Mock ManagedObjectAccessor moa;
  @Mock VirtualMachineConfigInfo vmci;
  @Mock VMPropertyHandler paramHandler;
  @Mock VirtualMachineConfigSpec vmConfigSpec;
  @Mock ServiceContent serviceContent;
  @Mock VimPortType vimPortType;
  @Mock ServiceConnection serviceConnection;

  @Before
  public void setUp() throws Exception {
    PowerMockito.spy(NetworkManager.class);
    MockitoAnnotations.initMocks(this);
    initMocks();
  }

  private void initMocks() throws Exception {
    doReturn(moa).when(vmw).getServiceUtil();
    doReturn(vmci).when(moa).getDynamicProperty(vmwInstance, "config");
    doReturn(serviceConnection).when(vmw).getConnection();
    doReturn(serviceContent).when(serviceConnection).getServiceContent();
    doReturn(vimPortType).when(serviceConnection).getService();
  }

  @Test
  public void getNumberOfNICs() throws Exception {
    // given
    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    VirtualEthernetCard etc2 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);
    etcs.add(etc2);
    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);

    // when
    int result = NetworkManager.getNumberOfNICs(vmw, vmwInstance);

    // then
    assertEquals(2, result);
  }

  @Test
  public void getNetworkName() throws Exception {
    // given
    NetworkSummary summary = mock(NetworkSummary.class);
    List<ManagedObjectReference> mors = new ArrayList<ManagedObjectReference>();
    mors.add(mor);

    doReturn(mors).when(moa).getDynamicProperty(vmwInstance, "network");
    doReturn("test").when(moa).getDynamicProperty(mor, "name");
    doReturn(summary).when(moa).getDynamicProperty(mor, "summary");

    // when
    String result = NetworkManager.getNetworkName(vmw, vmwInstance, 1);

    // then
    assertEquals("test", result);
  }

  @Test(expected = Exception.class)
  public void getNetworkName_exception() throws Exception {
    // given
    NetworkSummary summary = mock(NetworkSummary.class);
    List<ManagedObjectReference> mors = new ArrayList<ManagedObjectReference>();
    mors.add(mor);

    doReturn(mors).when(moa).getDynamicProperty(vmwInstance, "network");
    doReturn(null).when(moa).getDynamicProperty(mor, "name");
    doReturn(summary).when(moa).getDynamicProperty(mor, "summary");

    // when
    NetworkManager.getNetworkName(vmw, vmwInstance, 1);
  }

  @Test(expected = Exception.class)
  public void configureNetworkAdapter_differentNumbersOfNics() throws Exception {
    // given
    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    VirtualEthernetCard etc2 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);
    etcs.add(etc2);
    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);

    doReturn(1).when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    // when
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmwInstance);

    // then
  }

  @Test(expected = Exception.class)
  public void configureNetworkAdapter_portgroup_noSwitchSpecified() throws Exception {
    // given
    PortgroupIpSettings pis = mock(PortgroupIpSettings.class);
    Portgroup pg = mock(Portgroup.class);
    DistributedVirtualSwitch dvs = mock(DistributedVirtualSwitch.class);

    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);

    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);
    PowerMockito.doReturn(pis)
        .when(NetworkManager.class, "createPortgroupIpSettings", paramHandler, 1);

    doReturn("1").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    doReturn("test").when(paramHandler).getNetworkAdapter(1);
    doReturn("test").when(paramHandler).getPortGroup(1);
    doReturn("test").when(paramHandler).getSwitchUUID(1);
    doReturn(pg).when(pis).getPortgroup();
    doReturn(dvs).when(pis).getDvs();
    doReturn("test").when(dvs).getUuid();
    doReturn("").when(pg).getUuid();

    // when
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmwInstance);
    // then
  }

  @Test
  public void configureNetworkAdapter_portgroup() throws Exception {
    // given
    PortgroupIpSettings pis = mock(PortgroupIpSettings.class);
    Portgroup pg = mock(Portgroup.class);
    DistributedVirtualSwitch dvs = mock(DistributedVirtualSwitch.class);
    VirtualDevice device = mock(VirtualDevice.class);

    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);

    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);
    PowerMockito.doReturn(pis)
        .when(NetworkManager.class, "createPortgroupIpSettings", paramHandler, 1);
    PowerMockito.doReturn(mor)
        .when(NetworkManager.class, "getPortGroupFromHost", vmw, vmwInstance, etc1, "test", "test");
    PowerMockito.doNothing()
        .when(NetworkManager.class, "replaceNetworkAdapter", vmConfigSpec, device, mor, "test");

    doReturn("1").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    doReturn("test").when(paramHandler).getNetworkAdapter(1);
    doReturn("test").when(paramHandler).getPortGroup(1);
    doReturn("test").when(paramHandler).getSwitchUUID(1);
    doReturn(pg).when(pis).getPortgroup();
    doReturn(dvs).when(pis).getDvs();
    doReturn("test").when(dvs).getUuid();
    doReturn("test").when(pg).getUuid();

    // when
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmwInstance);

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.replaceNetworkAdapter(any(), any(), any(), anyString());
  }

  @Test
  public void configureNetworkAdapter_newNetworkName() throws Exception {
    // given
    VirtualDevice device = mock(VirtualDevice.class);

    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);

    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);
    PowerMockito.doReturn(mor)
        .when(NetworkManager.class, "getNetworkFromHost", vmw, vmwInstance, etc1, "test");
    PowerMockito.doReturn("test1")
        .when(NetworkManager.class, "getNetworkName", vmw, vmwInstance, 1);
    PowerMockito.doNothing()
        .when(NetworkManager.class, "replaceNetworkAdapter", vmConfigSpec, device, mor, "test");

    doReturn("1").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    doReturn("test").when(paramHandler).getNetworkAdapter(1);
    doReturn("").when(paramHandler).getPortGroup(1);
    doReturn("").when(paramHandler).getSwitchUUID(1);

    // when
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmwInstance);

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.replaceNetworkAdapter(any(), any(), any(), anyString());
  }

  @Test
  public void configureNetworkAdapter() throws Exception {
    // given

    VirtualEthernetCard etc1 = new VirtualEthernetCard();
    List<VirtualEthernetCard> etcs = new ArrayList<VirtualEthernetCard>();
    etcs.add(etc1);

    PowerMockito.doReturn(etcs).when(NetworkManager.class, "getNetworkAdapter", vmci);
    PowerMockito.doReturn(mor)
        .when(NetworkManager.class, "getNetworkFromHost", vmw, vmwInstance, etc1, "test");
    PowerMockito.doReturn("test").when(NetworkManager.class, "getNetworkName", vmw, vmwInstance, 1);
    PowerMockito.doNothing().when(NetworkManager.class, "connectNIC", vmConfigSpec, etc1);

    doReturn("1").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    doReturn("test").when(paramHandler).getNetworkAdapter(1);
    doReturn("").when(paramHandler).getPortGroup(1);
    doReturn("").when(paramHandler).getSwitchUUID(1);

    // when
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmwInstance);

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.connectNIC(any(), any());
  }

  @Test
  public void getPortGroupFromHost() throws Exception {
    // given
    VirtualEthernetCard etc = new VirtualEthernetCard();
    List<ManagedObjectReference> moas = new ArrayList<ManagedObjectReference>();
    moas.add(mor);
    VirtualDevice vd = new VirtualDevice();

    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    ManagedObjectReference sw = mock(ManagedObjectReference.class);
    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);

    doReturn(moas).when(moa).getDynamicProperty(sw, "portgroup");
    doReturn("key").when(moa).getDynamicProperty(mor, "key");
    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmwInstance, "runtime");
    doReturn(sw).when(vimPortType).queryDvsByUuid(any(), anyString());
    doReturn(hostRef).when(vmRuntimeInfo).getHost();

    PowerMockito.doReturn(vd).when(NetworkManager.class, "prepareDvNicDevice", etc, mor, "key");

    // when
    NetworkManager.getPortGroupFromHost(vmw, vmwInstance, etc, "switch", "key");

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.prepareDvNicDevice(any(), any(), anyString());
  }

  @Test(expected = Exception.class)
  public void getPortGroupFromHost_excpetion() throws Exception {
    // given
    VirtualEthernetCard etc = new VirtualEthernetCard();
    List<ManagedObjectReference> moas = new ArrayList<ManagedObjectReference>();
    moas.add(mor);
    VirtualDevice vd = new VirtualDevice();

    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    ManagedObjectReference sw = mock(ManagedObjectReference.class);
    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);

    doReturn(moas).when(moa).getDynamicProperty(sw, "portgroup");
    doReturn("key").when(moa).getDynamicProperty(mor, "key");
    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmwInstance, "runtime");
    doReturn(sw).when(vimPortType).queryDvsByUuid(any(), anyString());
    doReturn(hostRef).when(vmRuntimeInfo).getHost();

    PowerMockito.doReturn(vd).when(NetworkManager.class, "prepareDvNicDevice", etc, mor, "key");

    // when
    NetworkManager.getPortGroupFromHost(vmw, vmwInstance, etc, "switch", "group");
  }

  @Test
  public void getNetworkFromHost() throws Exception {
    // given
    VirtualEthernetCard etc = new VirtualEthernetCard();
    List<ManagedObjectReference> networkRefList = new ArrayList<ManagedObjectReference>();
    networkRefList.add(mor);
    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);

    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmwInstance, "runtime");
    doReturn(networkRefList).when(moa).getDynamicProperty(hostRef, "network");
    doReturn("test").when(moa).getDynamicProperty(mor, "name");
    doReturn(hostRef).when(vmRuntimeInfo).getHost();

    // when
    ManagedObjectReference result =
        NetworkManager.getNetworkFromHost(vmw, vmwInstance, etc, "test");

    // then
    assertEquals(mor, result);
  }

  @Test(expected = Exception.class)
  public void getNetworkFromHost_exception() throws Exception {
    // given
    VirtualEthernetCard etc = new VirtualEthernetCard();
    List<ManagedObjectReference> networkRefList = new ArrayList<ManagedObjectReference>();
    networkRefList.add(mor);
    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);

    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmwInstance, "runtime");
    doReturn(networkRefList).when(moa).getDynamicProperty(hostRef, "network");
    doReturn("test1").when(moa).getDynamicProperty(mor, "name");
    doReturn(hostRef).when(vmRuntimeInfo).getHost();

    // when
    ManagedObjectReference result =
        NetworkManager.getNetworkFromHost(vmw, vmwInstance, etc, "test");

    // then
    assertEquals(mor, result);
  }

  @Test
  public void prepareDvNicDevice() throws Exception {

    // given
    VirtualEthernetCard card = new VirtualEthernetCard();

    VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking =
        new VirtualEthernetCardDistributedVirtualPortBackingInfo();
    DistributedVirtualSwitchPortConnection dvPortConnection =
        new DistributedVirtualSwitchPortConnection();

    PowerMockito.doReturn(dvPortBacking)
        .when(NetworkManager.class, "createVirtualEthernetCardDistributedVirtualPortBackingInfo");
    PowerMockito.doReturn(dvPortConnection)
        .when(NetworkManager.class, "createDistributedVirtualSwitchPortConnection");

    // when
    VirtualDevice result = NetworkManager.prepareDvNicDevice(card, mor, "uuid");

    // then
    assertEquals(card, result);
  }

  @Test
  public void getNetworkAdapter() {
    // given
    VirtualHardware hardware = mock(VirtualHardware.class);
    List<VirtualDevice> devices = new ArrayList<VirtualDevice>();
    VirtualEthernetCard card = new VirtualEthernetCard();
    devices.add(card);

    doReturn(hardware).when(vmci).getHardware();
    doReturn(devices).when(hardware).getDevice();

    // when
    List<VirtualEthernetCard> expected = NetworkManager.getNetworkAdapter(vmci);

    // then
    assertEquals(card, expected.get(0));
  }

  @Test
  public void replaceNetworkAdapter_portgroup() throws Exception {
    // given
    VirtualDevice oldNIC = mock(VirtualDevice.class);

    VirtualEthernetCardDistributedVirtualPortBackingInfo vdbi =
        mock(VirtualEthernetCardDistributedVirtualPortBackingInfo.class);

    VirtualEthernetCardDistributedVirtualPortBackingInfo dvPortBacking =
        new VirtualEthernetCardDistributedVirtualPortBackingInfo();

    DistributedVirtualSwitchPortConnection dvPortConnection =
        mock(DistributedVirtualSwitchPortConnection.class);

    PowerMockito.doReturn(dvPortBacking)
        .when(NetworkManager.class, "createVirtualEthernetCardDistributedVirtualPortBackingInfo");
    PowerMockito.doReturn(dvPortConnection)
        .when(NetworkManager.class, "createDistributedVirtualSwitchPortConnection");

    doReturn(vdbi).when(oldNIC).getBacking();
    doReturn(dvPortConnection).when(vdbi).getPort();
    doReturn("portKey").when(dvPortConnection).getPortgroupKey();
    doReturn("switchUUID").when(dvPortConnection).getSwitchUuid();

    // when
    NetworkManager.replaceNetworkAdapter(vmConfigSpec, oldNIC, mor, "testNetwork");

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.connectNIC(any(), any());
  }

  @Test
  public void replaceNetworkAdapter() throws Exception {
    // given
    VirtualDevice oldNIC = mock(VirtualDevice.class);
    VirtualDeviceBackingInfo vdbi = mock(VirtualDeviceBackingInfo.class);

    doReturn(vdbi).when(oldNIC).getBacking();

    // when
    NetworkManager.replaceNetworkAdapter(vmConfigSpec, oldNIC, mor, "testNetwork");

    // then
    PowerMockito.verifyStatic(VerificationModeFactory.times(1));
    NetworkManager.connectNIC(any(), any());
  }
}
