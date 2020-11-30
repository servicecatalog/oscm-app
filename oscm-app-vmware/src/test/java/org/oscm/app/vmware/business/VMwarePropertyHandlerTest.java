/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 2016-05-24
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.LoggerMocking;
import org.oscm.app.vmware.business.model.Cluster;
import org.oscm.app.vmware.business.model.VCenter;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.persistence.VMwareNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.impl.SimpleLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({VMPropertyHandler.class})
public class VMwarePropertyHandlerTest {

  private HashMap<String, Setting> parameters;
  private HashMap<String, Setting> configSettings;
  private ProvisioningSettings settings;
  private VMPropertyHandler propertyHandler;
  private DataAccessService das;
  private PortgroupIpSettings ipSettings;
  private VMwareNetwork network;
  private Cluster cluster;
  private TaskInfo taskInfo;
  private SimpleLogger mogger;

  @Before
  public void before() {
    parameters = new HashMap<>();
    configSettings = new HashMap<>();
    settings = new ProvisioningSettings(parameters, configSettings, Messages.DEFAULT_LOCALE);
    propertyHandler = spy(new VMPropertyHandler(settings));

    das = mock(DataAccessService.class);
    ipSettings = mock(PortgroupIpSettings.class);
    network = mock(VMwareNetwork.class);
    cluster = mock(Cluster.class);
    taskInfo = mock(TaskInfo.class);
    mogger = LoggerMocking.setDebugEnabledFor(VMPropertyHandler.class);
    doReturn(das).when(propertyHandler).getDataAccessService();
    propertyHandler = PowerMockito.spy(new VMPropertyHandler(settings));
  }

  @Test
  public void testNetworkParameter() {
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC1_NETWORK_SETTINGS, "DHCP"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC2_NETWORK_SETTINGS, "DHCP"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC3_NETWORK_SETTINGS, "DHCP"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC4_NETWORK_SETTINGS, "DHCP"));

    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_GATEWAY,
            new Setting(VMPropertyHandler.TS_NIC1_GATEWAY, "127.0.0.1"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_GATEWAY,
            new Setting(VMPropertyHandler.TS_NIC2_GATEWAY, "127.0.0.2"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_GATEWAY,
            new Setting(VMPropertyHandler.TS_NIC3_GATEWAY, "127.0.0.3"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_GATEWAY,
            new Setting(VMPropertyHandler.TS_NIC4_GATEWAY, "127.0.0.4"));

    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_IP_ADDRESS,
            new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "127.1.0.1"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_IP_ADDRESS,
            new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "127.1.0.2"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_IP_ADDRESS,
            new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "127.1.0.3"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_IP_ADDRESS,
            new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "127.1.0.4"));

    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_DNS_SERVER,
            new Setting(VMPropertyHandler.TS_NIC1_DNS_SERVER, "127.2.0.1"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_DNS_SERVER,
            new Setting(VMPropertyHandler.TS_NIC2_DNS_SERVER, "127.2.0.2"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_DNS_SERVER,
            new Setting(VMPropertyHandler.TS_NIC3_DNS_SERVER, "127.2.0.3"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_DNS_SERVER,
            new Setting(VMPropertyHandler.TS_NIC4_DNS_SERVER, "127.2.0.4"));

    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_DNS_SUFFIX,
            new Setting(VMPropertyHandler.TS_NIC1_DNS_SUFFIX, "suffix1"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_DNS_SUFFIX,
            new Setting(VMPropertyHandler.TS_NIC2_DNS_SUFFIX, "suffix2"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_DNS_SUFFIX,
            new Setting(VMPropertyHandler.TS_NIC3_DNS_SUFFIX, "suffix3"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_DNS_SUFFIX,
            new Setting(VMPropertyHandler.TS_NIC4_DNS_SUFFIX, "suffix4"));

    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_SUBNET_MASK,
            new Setting(VMPropertyHandler.TS_NIC1_SUBNET_MASK, "255.255.1.0"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_SUBNET_MASK,
            new Setting(VMPropertyHandler.TS_NIC2_SUBNET_MASK, "255.255.2.0"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC3_SUBNET_MASK,
            new Setting(VMPropertyHandler.TS_NIC3_SUBNET_MASK, "255.255.3.0"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC4_SUBNET_MASK,
            new Setting(VMPropertyHandler.TS_NIC4_SUBNET_MASK, "255.255.4.0"));

    for (int i = 1; i < 5; i++) {
      propertyHandler.isAdapterConfiguredByDhcp(i);
      propertyHandler.getGateway(i);
      propertyHandler.getIpAddress(i);
      propertyHandler.getDNSServer(i);
      propertyHandler.getDNSSuffix(i);
      propertyHandler.getSubnetMask(i);
    }

    try {
      propertyHandler.isAdapterConfiguredByDhcp(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }

    try {
      propertyHandler.getGateway(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }

    try {
      propertyHandler.getIpAddress(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }

    try {
      propertyHandler.getDNSServer(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }

    try {
      propertyHandler.getDNSSuffix(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }

    try {
      propertyHandler.getSubnetMask(5);
    } catch (IllegalArgumentException e) {
      assertTrue("NIC identifier 5 is out of range. Valid range is [1-4].".equals(e.getMessage()));
    }
  }

  @Test
  public void getVsphereAttributes_withParameter() {
    // given
    settings
        .getParameters()
        .put("VSPHERE_ATTRIBUTE_BACKUP", new Setting("VSPHERE_ATTRIBUTE_BACKUP", "true"));

    // when
    HashMap<String, String> attributes = propertyHandler.getVsphereAttributes();

    // then
    assertEquals("true", attributes.get("BACKUP"));
  }

  @Test
  public void getVsphereAttributes_withCustomAttributes() {
    // given
    settings
        .getCustomAttributes()
        .put("VSPHERE_ATTRIBUTE_BACKUP", new Setting("VSPHERE_ATTRIBUTE_BACKUP", "true"));

    // when
    HashMap<String, String> attributes = propertyHandler.getVsphereAttributes();

    // then
    assertEquals("true", attributes.get("BACKUP"));
  }

  @Test
  public void getVsphereAttributes_withCustomAttributesAndParameter() {
    // given
    settings
        .getCustomAttributes()
        .put("VSPHERE_ATTRIBUTE_BACKUP", new Setting("VSPHERE_ATTRIBUTE_BACKUP", "false"));

    settings
        .getCustomAttributes()
        .put("VSPHERE_ATTRIBUTE_BACKUP", new Setting("VSPHERE_ATTRIBUTE_BACKUP", "true"));

    // when
    HashMap<String, String> attributes = propertyHandler.getVsphereAttributes();

    // then
    assertEquals("true", attributes.get("BACKUP"));
  }

  @Test
  public void getConfigDiskSpaceMB_sizeParameter() throws Exception {
    // given
    settings
        .getParameters()
        .put(VMPropertyHandler.TS_DISK_SIZE, new Setting(VMPropertyHandler.TS_DISK_SIZE, "17"));

    // when
    double diskSize = propertyHandler.getConfigDiskSpaceMB();

    // then
    assertTrue(diskSize == (17.0 * 1024));
  }

  @Test
  public void getConfigDiskSpaceMB_parameterMissing() throws Exception {
    // given

    // when
    double diskSize = propertyHandler.getConfigDiskSpaceMB();

    // then
    assertTrue(diskSize == .0);
  }

  @Test(expected = APPlatformException.class)
  public void getConfigDiskSpaceMB_parameterInvalid() throws Exception {
    // given
    settings
        .getParameters()
        .put(VMPropertyHandler.TS_DISK_SIZE, new Setting(VMPropertyHandler.TS_DISK_SIZE, "12abc"));

    // when
    propertyHandler.getConfigDiskSpaceMB();
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNetworkAdapter_0() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    propertyHandler.getNetworkAdapter(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getNetworkAdapter_greater_4() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    propertyHandler.getNetworkAdapter(5);
  }

  @Test
  public void getNetworkAdapter_1() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    String adapter = propertyHandler.getNetworkAdapter(1);

    // then
    assertEquals("adapter 1", adapter);
  }

  @Test
  public void getNetworkAdapter_2() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    String adapter = propertyHandler.getNetworkAdapter(2);

    // then
    assertEquals("adapter 2", adapter);
  }

  @Test
  public void getNetworkAdapter_3() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    String adapter = propertyHandler.getNetworkAdapter(3);

    // then
    assertEquals("adapter 3", adapter);
  }

  @Test
  public void getNetworkAdapter_4() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER, "adapter 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC2_NETWORK_ADAPTER, "adapter 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC3_NETWORK_ADAPTER, "adapter 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER,
        new Setting(VMPropertyHandler.TS_NIC4_NETWORK_ADAPTER, "adapter 4"));

    // when
    String adapter = propertyHandler.getNetworkAdapter(4);

    // then
    assertEquals("adapter 4", adapter);
  }

  @Test
  public void releaseManuallyDefinedIPAddresses() throws Exception {
    // given
    parameters.put(
        VMPropertyHandler.TS_NUMBER_OF_NICS, new Setting(VMPropertyHandler.TS_NUMBER_OF_NICS, "1"));
    doReturn(Boolean.TRUE).when(propertyHandler).isAdapterConfiguredByDatabase(1);
    doReturn("ipaddress").when(propertyHandler).getIpAddress(anyInt());
    doReturn("site").when(propertyHandler).getTargetVCenterServer();
    doReturn("datacenter").when(propertyHandler).getTargetDatacenter();
    doReturn("cluster").when(propertyHandler).getTargetCluster();
    doReturn("vlan").when(propertyHandler).getVLAN(anyInt());
    PowerMockito.whenNew(DataAccessService.class).withAnyArguments().thenReturn(das);
    doNothing().when(das).releaseIPAddress(anyString(), anyString(), anyString(), anyString(), anyString());
    // when
    propertyHandler.releaseManuallyDefinedIPAddresses();
    // then
    verify(das, times(1))
        .releaseIPAddress(eq("site"), eq("datacenter"), eq("cluster"), eq("vlan"), eq("ipaddress"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getIpAddress_NIC0() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    propertyHandler.getIpAddress(0);
  }

  @Test
  public void getIpAddress_NIC1() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    String ipAddress = propertyHandler.getIpAddress(1);

    // then
    assertEquals("ip address 1", ipAddress);
  }

  @Test
  public void getIpAddress_NIC1_undefined() {
    // given

    // when
    String ipAddress = propertyHandler.getIpAddress(1);

    // then
    assertNull(ipAddress);
  }

  @Test
  public void getIpAddress_NIC2() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    String ipAddress = propertyHandler.getIpAddress(2);

    // then
    assertEquals("ip address 2", ipAddress);
  }

  @Test
  public void getIpAddress_NIC3() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    String ipAddress = propertyHandler.getIpAddress(3);

    // then
    assertEquals("ip address 3", ipAddress);
  }

  @Test
  public void getIpAddress_NIC4() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    String ipAddress = propertyHandler.getIpAddress(4);

    // then
    assertEquals("ip address 4", ipAddress);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getIpAddress_greater_4() {
    // given
    parameters.put(
        VMPropertyHandler.TS_NIC1_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC1_IP_ADDRESS, "ip address 1"));
    parameters.put(
        VMPropertyHandler.TS_NIC2_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC2_IP_ADDRESS, "ip address 2"));
    parameters.put(
        VMPropertyHandler.TS_NIC3_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC3_IP_ADDRESS, "ip address 3"));
    parameters.put(
        VMPropertyHandler.TS_NIC4_IP_ADDRESS,
        new Setting(VMPropertyHandler.TS_NIC4_IP_ADDRESS, "ip address 4"));

    // when
    propertyHandler.getIpAddress(5);
  }

  @Test
  public void testGetNetworkSettingsFromDatabase() throws Exception {
    // given
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC1_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC1_NETWORK_SETTINGS, "DATABASE"));
    settings
        .getParameters()
        .put(
            VMPropertyHandler.TS_NIC2_NETWORK_SETTINGS,
            new Setting(VMPropertyHandler.TS_NIC2_NETWORK_SETTINGS, "PORTGROUPIPPOOL"));
    PowerMockito.when(propertyHandler.getServiceSetting(anyString())).thenReturn("2");
    doNothing().when(propertyHandler).configureByDatabase(anyInt());
    doNothing().when(propertyHandler).configureByPortgroupIPPool(anyInt());
    // when
    propertyHandler.getNetworkSettingsFromDatabase();
    //then
    verify(propertyHandler, times(1)).configureByDatabase(1);
    verify(propertyHandler, times(1)).configureByPortgroupIPPool(2);
  }

  @Test
  public void testConfigureByPortgroupIPPool() throws Exception {
    // given
    PowerMockito.whenNew(PortgroupIpSettings.class).withAnyArguments().thenReturn(ipSettings);
    when(ipSettings.getIpAdressFromIpPool()).thenReturn("127.0.0.1");
    // when
    propertyHandler.configureByPortgroupIPPool(1);
    //then
    assertEquals("127.0.0.1", settings.getParameters().get(VMPropertyHandler.TS_NIC1_IP_ADDRESS).getValue());
  }

  @Test
  public void testConfigureByDatabase_value_1() throws Exception {
    // given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getVLANwithMostIPs(anyString(), anyString(), anyString())).thenReturn("vlan");
    when(das.getNetworkSettings(anyString(), anyString(), anyString(), anyString())).thenReturn(network);
    when(network.getDnsSuffix()).thenReturn("suf");
    // when
    propertyHandler.configureByDatabase(1);
    //then
    assertEquals("suf", settings.getParameters().get(VMPropertyHandler.TS_NIC1_DNS_SUFFIX).getValue());
  }

  @Test
  public void testConfigureByDatabase_value_2() throws Exception {
    // given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getVLANwithMostIPs(anyString(), anyString(), anyString())).thenReturn("vlan");
    when(das.getNetworkSettings(anyString(), anyString(), anyString(), anyString())).thenReturn(network);
    when(network.getDnsSuffix()).thenReturn("suf");
    // when
    propertyHandler.configureByDatabase(2);
    //then
    assertEquals("suf", settings.getParameters().get(VMPropertyHandler.TS_NIC2_DNS_SUFFIX).getValue());
  }

  @Test
  public void testConfigureByDatabase_value_3() throws Exception {
    // given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getVLANwithMostIPs(anyString(), anyString(), anyString())).thenReturn("vlan");
    when(das.getNetworkSettings(anyString(), anyString(), anyString(), anyString())).thenReturn(network);
    when(network.getDnsSuffix()).thenReturn("suf");
    // when
    propertyHandler.configureByDatabase(3);
    //then
    assertEquals("suf", settings.getParameters().get(VMPropertyHandler.TS_NIC3_DNS_SUFFIX).getValue());
  }

  @Test
  public void testConfigureByDatabase_value_4() throws Exception {
    // given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getVLANwithMostIPs(anyString(), anyString(), anyString())).thenReturn("vlan");
    when(das.getNetworkSettings(anyString(), anyString(), anyString(), anyString())).thenReturn(network);
    when(network.getDnsSuffix()).thenReturn("suf");
    // when
    propertyHandler.configureByDatabase(4);
    //then
    assertEquals("suf", settings.getParameters().get(VMPropertyHandler.TS_NIC4_DNS_SUFFIX).getValue());
  }

  @Test(expected = APPlatformException.class)
  public void testConfigureByDatabase_ThrowException() throws Exception {
    // given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    // when
    propertyHandler.configureByDatabase(4);
  }

  @Test
  public void testGetDataDisksMB() {
    // given
    PowerMockito.when(propertyHandler.getServiceSetting(anyString())).thenReturn("1", "2", null);
    // when
    Double[] result = propertyHandler.getDataDisksMB();
    //then
    verify(propertyHandler, times(4)).getServiceSetting(anyString());
    assertEquals(2, result.length);
  }

  @Test
  public void testGetDataDiskKey() {
    // given
    settings.getParameters().put("DATA_DISK_KEY_2", new Setting("DATA_DISK_KEY_2", "140"));
    // when
    int result = propertyHandler.getDataDiskKey(2);
    //then
    assertEquals(140, result);
  }

  @Test
  public void testSetDataDiskKey() {
    // when
    propertyHandler.setDataDiskKey(2, 140);
    //then
    assertEquals(140, propertyHandler.getDataDiskKey(2));
  }

  @Test
  public void testGetVLANs() throws Exception {
    //given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    // when
    propertyHandler.getVLANs(cluster);
    //then
    verify(das, times(1)).getVLANs(cluster);
  }

  @Test
  public void testGetInstanceNameCustom() throws Exception {
    //given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getVCenterIdentifier(anyString())).thenReturn("name");
    PowerMockito.when(propertyHandler, "getDatacenterId").thenReturn("_datacenter");
    when(das.getNextSequenceNumber(anyInt(), anyString())).thenReturn("_id");
    // when
    String result = Whitebox.invokeMethod(propertyHandler, "getInstanceNameCustom", "${VC}${DC}${ID3}${ID4}${ID5}${ID6}${ID7}${ID8}${ID10}${ID12}");
    //then
    verify(das, times(8)).getNextSequenceNumber(anyInt(), anyString());
    assertEquals("name_datacenter_id_id_id_id_id_id_id_id", result);
  }

  @Test
  public void testSetTask() throws Exception {
    // when
    propertyHandler.setTask(null);
    //then
    assertEquals("", settings.getParameters().get(VMPropertyHandler.TASK_KEY).getValue());
    PowerMockito.verifyPrivate(propertyHandler, times(1)).invoke("logTaskInfo", null);
  }

  @Test
  public void testLogTaskInfo() throws Exception {
    //given
    when(taskInfo.getState()).thenReturn(TaskInfoState.SUCCESS);
    // when
    Whitebox.invokeMethod(propertyHandler, "logTaskInfo", taskInfo);
    //then
    verify(mogger, times(1)).debug("Save task info key: null name: null target: null state: SUCCESS progress: 100% description:  queue-time:  start-time:  complete-time: ");
  }

  @Test
  public void testGetHostLoadBalancerConfig() {
    //given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getHostLoadBalancerConfig(anyString(), anyString(), anyString())).thenReturn("Test xml content");
    // when
    String result = propertyHandler.getHostLoadBalancerConfig();
    //then
    assertEquals("Test xml content", result);
  }

  @Test(expected = RuntimeException.class)
  public void testGetHostLoadBalancerConfigThrowException() {
    //given
    PowerMockito.when(propertyHandler.getDataAccessService()).thenReturn(das);
    when(das.getHostLoadBalancerConfig(anyString(), anyString(), anyString())).thenReturn("");
    // when
    propertyHandler.getHostLoadBalancerConfig();
  }

  @Test
  public void testGetDataDiskMountPointParameterKeys() {
    //given
    settings.getParameters().put("DATA_DISK_TARGET_18281", new Setting("DATA_DISK_KEY_1", "120"));
    settings.getParameters().put("DATA_DISK_TARGET_14465", new Setting("DATA_DISK_KEY_2", "140"));
    // when
    List<String> result = propertyHandler.getDataDiskMountPointParameterKeys();
    //then
    assertEquals(2, result.size());
    assertEquals("DATA_DISK_TARGET_14465", result.get(0));
  }

  @Test
  public void testGetDataDiskSizeParameterKeys() {
    //given
    settings.getParameters().put("DATA_DISK_SIZE_18281", new Setting("DATA_DISK_KEY_1", "120"));
    settings.getParameters().put("DATA_DISK_SIZE_14465", new Setting("DATA_DISK_KEY_2", "140"));
    // when
    List<String> result = propertyHandler.getDataDiskSizeParameterKeys();
    //then
    assertEquals(2, result.size());
    assertEquals("DATA_DISK_SIZE_14465", result.get(0));
  }

  @Test
  public void testGetDataDisksAsString() throws Exception {
    //given
    Double[] discMB = {1001.22, 2048.52};
    when(propertyHandler.getDataDisksMB()).thenReturn(discMB);
    // when
    String result = propertyHandler.getDataDisksAsString();
    //then
    assertEquals("0 GB/1 GB/2 GB", result);
  }

  @Test
  public void testGetResponsibleUserAsString() {
    //given
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("USER");
    // when
    String result = propertyHandler.getResponsibleUserAsString("en");
    //then
    assertEquals("Responsible user: USER", result);
  }

  @Test
  public void testGetResponsibleUserAsStringReturnNull() {
    // when //then
    assertNull(propertyHandler.getResponsibleUserAsString("en"));
  }

  @Test
  public void testFormatMBasGB() {
    // when
    String result = propertyHandler.formatMBasGB(6254.2554);
    //then
    assertEquals("6.1 GB", result);
  }

  @Test
  public void testGetTargetFolder() {
    //given
    settings.setOrganizationId("15000");
    when(propertyHandler.getServiceSetting(anyString())).thenReturn("/oscm-app/folders/${ORGID}");
    // when
    String result = propertyHandler.getTargetFolder();
    //then
    assertEquals("/oscm-app/folders/15000", result);
  }
}
