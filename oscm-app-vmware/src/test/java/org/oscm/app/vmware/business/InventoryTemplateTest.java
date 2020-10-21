/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 19 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.balancer.LoadBalancerConfiguration;
import org.oscm.app.vmware.business.balancer.VMwareBalancer;
import org.oscm.app.vmware.business.model.VMwareHost;
import org.oscm.app.vmware.business.model.VMwareStorage;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.DatastoreHostMount;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.HostMountInfo;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** @author worf */
public class InventoryTemplateTest {

  @Mock VMPropertyHandler paramHandler;
  @Mock VMwareClient vmw;
  @Mock ManagedObjectReference vmDataCenter;
  @Mock ManagedObjectAccessor moa;

  InventoryTemplate template;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    template = new InventoryTemplate(vmw, paramHandler);
    template = Mockito.spy(template);

    setUpMocks();
  }

  private void setUpMocks() {
    doReturn(moa).when(vmw).getServiceUtil();
  }

  @Test
  public void getHostAndStorageSpecFromBalancer() throws Exception {
    // given
    VMwareDatacenterInventory inventory = mock(VMwareDatacenterInventory.class);
    LoadBalancerConfiguration config = mock(LoadBalancerConfiguration.class);
    VMwareHost host = mock(VMwareHost.class);
    VMwareBalancer<VMwareHost> balancer = mock(VMwareBalancer.class);
    VMwareStorage storage = mock(VMwareStorage.class);
    ManagedObjectReference moa = mock(ManagedObjectReference.class);

    doReturn("datacenter").when(paramHandler).getTargetDatacenter();
    doReturn("cluster").when(paramHandler).getTargetCluster();
    doReturn("config").when(paramHandler).getHostLoadBalancerConfig();
    doReturn("storage").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_TARGET_STORAGE);
    doReturn("").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_TARGET_HOST);

    doReturn(inventory).when(template).readDatacenterInventory(vmw, "datacenter", "cluster");
    doReturn(config).when(template).createLoadBalancerConfiguration("config", inventory);
    doReturn(balancer).when(config).getBalancer();
    doReturn(host).when(balancer).next(paramHandler);
    doReturn(storage).when(host).getNextStorage(paramHandler);
    doReturn("host").when(host).getName();
    doReturn("storage").when(storage).getName();
    doReturn(moa).when(template).getVMHost(vmDataCenter, "host");
    doReturn(moa).when(template).getVMPool(anyString(), any());
    doReturn(moa).when(template).getVMDatastore(anyString(), anyString(), any());

    // when
    VirtualMachineRelocateSpec spec = template.getHostAndStorageSpec(vmDataCenter);
    // then
    assertEquals(moa, spec.getHost());
  }

  @Test
  public void getHostAndStorageSpecFromInventory() throws Exception {
    // given
    VMwareDatacenterInventory inventory = mock(VMwareDatacenterInventory.class);
    LoadBalancerConfiguration config = mock(LoadBalancerConfiguration.class);
    VMwareHost host = mock(VMwareHost.class);
    VMwareStorage storage = mock(VMwareStorage.class);
    ManagedObjectReference moa = mock(ManagedObjectReference.class);

    doReturn("datacenter").when(paramHandler).getTargetDatacenter();
    doReturn("cluster").when(paramHandler).getTargetCluster();
    doReturn("config").when(paramHandler).getHostLoadBalancerConfig();
    doReturn("").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_TARGET_STORAGE);
    doReturn("host").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_TARGET_HOST);

    doReturn(inventory).when(template).readDatacenterInventory(vmw, "datacenter", "cluster");
    doReturn(host).when(inventory).getHost("host");
    doReturn(storage).when(host).getNextStorage(paramHandler);
    doReturn("storage").when(storage).getName();
    doReturn(moa).when(template).getVMHost(vmDataCenter, "host");
    doReturn(moa).when(template).getVMPool(anyString(), any());
    doReturn(moa).when(template).getVMDatastore(anyString(), anyString(), any());

    // when
    VirtualMachineRelocateSpec spec = template.getHostAndStorageSpec(vmDataCenter);
    // then
    assertEquals(moa, spec.getHost());
  }

  @Test(expected = APPlatformException.class)
  public void getVMDatastore_exception() throws Exception {
    List<Object> mors = new ArrayList<Object>();
    ManagedObjectReference mor = mock(ManagedObjectReference.class);
    String mor1 = "";
    ManagedObjectReference vmHost = mock(ManagedObjectReference.class);
    mors.add(mor1);
    mors.add(mor);

    doReturn(mors).when(moa).getDynamicProperty(vmHost, "datastore");
    doReturn("name").when(moa).getDynamicProperty((ManagedObjectReference) mor, "summary.name");

    // when
    template.getVMDatastore("", "", vmHost);

    // then
  }

  @Test
  public void getVMDatastore() throws Exception {
    List<Object> mors = new ArrayList<Object>();
    ManagedObjectReference mor = mock(ManagedObjectReference.class);
    String mor1 = "";
    ManagedObjectReference vmHost = mock(ManagedObjectReference.class);
    mors.add(mor1);
    mors.add(mor);

    doReturn(mors).when(moa).getDynamicProperty(vmHost, "datastore");
    doReturn("dsname").when(moa).getDynamicProperty((ManagedObjectReference) mor, "summary.name");

    // when
    ManagedObjectReference result = template.getVMDatastore("dsname", "", vmHost);

    // then
    assertEquals(mor, result);
  }

  @Test
  public void getVMPool() throws Exception {
    // given
    ManagedObjectReference vmHost = mock(ManagedObjectReference.class);
    ManagedObjectReference vmHostCluster = mock(ManagedObjectReference.class);
    ManagedObjectReference vmPool = mock(ManagedObjectReference.class);

    doReturn(vmHostCluster).when(moa).getDynamicProperty(vmHost, "parent");
    doReturn(vmPool).when(moa).getDecendentMoRef(vmHostCluster, "ResourcePool", "Resources");

    // when
    ManagedObjectReference result = template.getVMPool("", vmHost);
    // then
    assertEquals(vmPool, result);
  }

  @Test(expected = APPlatformException.class)
  public void getVMPool_exception() throws Exception {
    // given
    ManagedObjectReference vmHost = mock(ManagedObjectReference.class);
    ManagedObjectReference vmHostCluster = mock(ManagedObjectReference.class);

    doReturn(vmHostCluster).when(moa).getDynamicProperty(vmHost, "parent");
    doReturn(null).when(moa).getDecendentMoRef(vmHostCluster, "ResourcePool", "Resources");

    // when
    template.getVMPool("", vmHost);
    // then
  }

  @Test(expected = APPlatformException.class)
  public void getVMHost_exception()
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    // given
    ManagedObjectReference vmDataCenter = mock(ManagedObjectReference.class);

    doReturn(null).when(moa).getDecendentMoRef(vmDataCenter, "HostSystem", "");
    // then
    template.getVMHost(vmDataCenter, "");
  }

  @Test
  public void getVMHost()
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    // given
    ManagedObjectReference vmDataCenter = mock(ManagedObjectReference.class);
    ManagedObjectReference vmHost = mock(ManagedObjectReference.class);

    doReturn(vmHost).when(moa).getDecendentMoRef(vmDataCenter, "HostSystem", "");

    // when
    ManagedObjectReference result = template.getVMHost(vmDataCenter, "");

    // then
    assertEquals(vmHost, result);
  }

  @Test
  public void readDatacenterInventory() throws Exception {
    // given
    List<ManagedObjectReference> hostMoRefs = new ArrayList<ManagedObjectReference>();
    List<DynamicProperty> dps = new ArrayList<DynamicProperty>();
    ManagedObjectReference dcMoRef = mock(ManagedObjectReference.class);
    ManagedObjectReference clusterMoRef = mock(ManagedObjectReference.class);
    ManagedObjectReference host1 = mock(ManagedObjectReference.class);
    ManagedObjectReference host2 = mock(ManagedObjectReference.class);
    hostMoRefs.add(host1);
    hostMoRefs.add(host2);

    doReturn(dcMoRef).when(moa).getDecendentMoRef(null, "Datacenter", "");
    doReturn(clusterMoRef).when(moa).getDecendentMoRef(dcMoRef, "ClusterComputeResource", "");
    doReturn(hostMoRefs).when(moa).getDynamicProperty(clusterMoRef, "host");
    doReturn(dps).when(template).setHostSystemPropertyToInventory(any(), any(), any());
    doReturn("host").when(template).getHostName(dps);
    doNothing().when(template).setStoragePropertyToInventory(any(), any(), any(), anyString());
    doNothing().when(template).setVMToInventory(any(), any(), any());

    // when
    template.readDatacenterInventory(vmw, "", "");
    // then
    verify(template, times(2)).setStoragePropertyToInventory(any(), any(), any(), anyString());
    verify(template, times(2)).setVMToInventory(any(), any(), any());
  }

  @Test
  public void setVMToInventory() throws Exception {
    // given
    List<ManagedObjectReference> vmRefs = new ArrayList<ManagedObjectReference>();
    ManagedObjectReference mor = mock(ManagedObjectReference.class);
    vmRefs.add(mor);
    VMwareDatacenterInventory inventory = mock(VMwareDatacenterInventory.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);

    doReturn(vmRefs).when(moa).getDynamicProperty(hostRef, "vm");
    doReturn(new ArrayList<DynamicProperty>())
        .when(moa)
        .getDynamicProperty(
            mor,
            new String[] {
              "name", "summary.config.memorySizeMB", "summary.config.numCpu", "runtime.host"
            });

    // when
    template.setVMToInventory(moa, inventory, hostRef);

    // then
    verify(inventory, times(1)).addVirtualMachine(any(), any());
  }

  @Test
  public void setStoragePropertyToInventory() throws Exception {

    // given
    List<ManagedObjectReference> storageRefs = new ArrayList<ManagedObjectReference>();
    ManagedObjectReference mor = mock(ManagedObjectReference.class);
    storageRefs.add(mor);

    List<DatastoreHostMount> hostMounts = new ArrayList<DatastoreHostMount>();
    DatastoreHostMount hostMount = mock(DatastoreHostMount.class);
    hostMounts.add(hostMount);
    ManagedObjectReference hostMor = mock(ManagedObjectReference.class);

    VMwareDatacenterInventory inventory = mock(VMwareDatacenterInventory.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    HostMountInfo info = mock(HostMountInfo.class);

    doReturn(storageRefs).when(moa).getDynamicProperty(hostRef, "datastore");
    doReturn(new ArrayList<DynamicProperty>())
        .when(moa)
        .getDynamicProperty(
            mor, new String[] {"summary.name", "summary.capacity", "summary.freeSpace"});
    doReturn(hostMounts).when(moa).getDynamicProperty(mor, "host");
    doReturn("storageName").when(template).getStorageName(any());
    doReturn("name").when(moa).getDynamicProperty(hostMor, "name");
    doReturn(hostMor).when(hostMount).getKey();
    doReturn(info).when(hostMount).getMountInfo();
    doReturn(new Boolean(true)).when(info).isAccessible();
    doReturn(new Boolean(true)).when(info).isMounted();
    doReturn("notReadOnly").when(info).getAccessMode();

    // when
    template.setStoragePropertyToInventory(moa, inventory, hostRef, "name");

    // then
    verify(inventory, times(1)).addStorage(anyString(), any());
  }

  @Test
  public void getStorageName() {
    // given
    List<DynamicProperty> dps = new ArrayList<DynamicProperty>();
    DynamicProperty dp = new DynamicProperty();
    dp.setName("summary.name");
    dp.setVal("name");
    dps.add(dp);

    // when
    String result = template.getStorageName(dps);

    // then
    assertEquals("name", result);
  }

  @Test
  public void getHostName() {
    // given
    List<DynamicProperty> dps = new ArrayList<DynamicProperty>();
    DynamicProperty dp = new DynamicProperty();
    dp.setName("name");
    dp.setVal("name");
    dps.add(dp);

    // when
    String result = template.getHostName(dps);

    // then
    assertEquals("name", result);
  }

  @Test
  public void setHostSystemPropertyToInventory() throws Exception {
    // given
    VMwareDatacenterInventory inventory = mock(VMwareDatacenterInventory.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    doReturn(new ArrayList<DynamicProperty>())
        .when(moa)
        .getDynamicProperty(
            hostRef,
            new String[] {"name", "summary.hardware.memorySize", "summary.hardware.numCpuCores"});

    // when
    template.setHostSystemPropertyToInventory(moa, inventory, hostRef);

    // then
    verify(inventory, times(1)).addHostSystem(any());
  }
}
