/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 12 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.Description;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceFileBackingInfo;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualDiskFlatVer2BackingInfo;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;

/** @author worf */
public class DiskManagerTest {

  @Mock VMPropertyHandler paramHandler;
  @Mock VMwareClient vmw;
  @Mock VirtualMachineConfigSpec vmConfigSpec;
  @Mock VirtualMachineConfigInfo configSpec;
  @Mock ManagedObjectReference vmwInstance;
  @Mock ManagedObjectAccessor moa;
  @Mock VirtualDisk vd;
  @Mock VirtualDisk vd1;
  List<VirtualDevice> devices;

  @InjectMocks DiskManager diskManager;

  @Before
  public void setUp() {
    diskManager = Mockito.spy(new DiskManager(vmw, paramHandler));
    MockitoAnnotations.initMocks(this);
    initMocks();
  }

  public void initMocks() {
    doReturn(moa).when(vmw).getServiceUtil();
    devices = new ArrayList<VirtualDevice>();
    devices.add(vd);
    devices.add(vd1);
    doReturn(2).when(vd).getKey();
    doReturn(1).when(vd1).getKey();
    doReturn(2).when(vd).getUnitNumber();
    doReturn(1).when(vd1).getUnitNumber();
  }

  @Test
  public void reconfigureDisks() throws Exception {

    // given
    doReturn(configSpec).when(moa).getDynamicProperty(vmwInstance, "config");
    VirtualHardware vh = mock(VirtualHardware.class);
    doReturn(vh).when(configSpec).getHardware();
    doReturn(vd).when(diskManager).getVirtualDisk(any(), any());
    doNothing().when(diskManager).configureSystemDisk(any(), anyLong(), any());
    doNothing().when(diskManager).configureDataDisks(any(), any(), any());

    // when
    diskManager.reconfigureDisks(vmConfigSpec, vmwInstance);

    // then
    verify(diskManager, times(1)).configureSystemDisk(any(), anyLong(), any());
    verify(diskManager, times(1)).configureDataDisks(any(), any(), any());
  }

  @Test
  public void configureDataDisks_keyIsZero() throws Exception {
    // given
    VirtualDeviceConfigSpec spec = mock(VirtualDeviceConfigSpec.class);
    doReturn(new Double[] {(double) 1024}).when(paramHandler).getDataDisksMB();
    doReturn(spec).when(diskManager).createNewDataDisk(any(), anyLong(), anyInt(), anyInt());

    // when
    diskManager.configureDataDisks(vmConfigSpec, devices, vd);

    // then
    verify(paramHandler, times(1)).setDataDiskKey(anyInt(), anyInt());
  }

  @Test
  public void configureDataDisks_keyIsNotZero() throws Exception {
    // given
    doReturn(new Double[] {(double) 1024}).when(paramHandler).getDataDisksMB();
    doReturn(1).when(paramHandler).getDataDiskKey(anyInt());
    doNothing().when(diskManager).updateDiskConfiguration(any(), any(), anyInt(), anyLong());
    // when
    diskManager.configureDataDisks(vmConfigSpec, devices, vd);

    // then
    verify(diskManager, times(1)).updateDiskConfiguration(any(), any(), anyInt(), anyLong());
  }

  @Test(expected = Exception.class)
  public void updateDiskConfiguration_exception() throws Exception {
    // given
    Description des = mock(Description.class);
    doReturn(vd).when(diskManager).findDataDisk(any(), anyInt());
    doReturn((long) 512).doReturn((long) 2048).when(vd).getCapacityInKB();
    doReturn(des).when(vd).getDeviceInfo();
    doReturn("test").when(des).getLabel();
    // when
    diskManager.updateDiskConfiguration(vmConfigSpec, devices, 1, 1024);
    // then
  }

  @Test
  public void updateDiskConfiguration() throws Exception {
    // given
    Description des = mock(Description.class);
    VirtualDeviceConfigSpec spec = mock(VirtualDeviceConfigSpec.class);
    doReturn(vd).when(diskManager).findDataDisk(any(), anyInt());
    doReturn(spec).when(diskManager).createVirtualDeviceConfigSpec();
    doReturn((long) 512).when(vd).getCapacityInKB();
    doReturn(des).when(vd).getDeviceInfo();
    doReturn("test").when(des).getLabel();

    // when
    diskManager.updateDiskConfiguration(vmConfigSpec, devices, 1, 1024);

    // then
    verify(spec, times(1)).setDevice(vd);
  }

  @Test
  public void createNewDataDisk() throws Exception {
    // given
    ManagedObjectReference vmDatastore = mock(ManagedObjectReference.class);
    VirtualDeviceFileBackingInfo vdbi = mock(VirtualDeviceFileBackingInfo.class);
    VirtualDiskFlatVer2BackingInfo vdf = mock(VirtualDiskFlatVer2BackingInfo.class);
    VirtualDeviceConfigSpec spec = mock(VirtualDeviceConfigSpec.class);

    doReturn(spec).when(diskManager).createVirtualDeviceConfigSpec();
    doReturn(vdbi).when(vd).getBacking();
    doReturn(vmDatastore).when(vdbi).getDatastore();
    doReturn(vd1).when(diskManager).createVirtualDisk();
    doReturn(vdf).when(diskManager).createVirtualDiskFlatVer2BackingInfo();

    // when
    VirtualDeviceConfigSpec vdspe = diskManager.createNewDataDisk(vd, 1024, 1, 1);

    // then

    assertEquals(spec, vdspe);
  }

  @Test
  public void findDataDisk() {

    // given

    // when
    VirtualDisk disk = diskManager.findDataDisk(devices, 2);

    // then
    assertEquals(vd, disk);
  }

  @Test(expected = Exception.class)
  public void configureSystemDisk_exeption() throws Exception {
    // given
    Description des = mock(Description.class);
    VirtualDeviceConfigSpec spec = mock(VirtualDeviceConfigSpec.class);
    doReturn(vd).when(diskManager).findDataDisk(any(), anyInt());
    doReturn(spec).when(diskManager).createVirtualDeviceConfigSpec();
    doReturn((long) 2048).when(vd).getCapacityInKB();
    doReturn(des).when(vd).getDeviceInfo();
    doReturn("test").when(des).getLabel();

    // when
    diskManager.configureSystemDisk(vmConfigSpec, 1, vd);
  }

  public void configureSystemDisk() throws Exception {
    // given
    Description des = mock(Description.class);
    VirtualDeviceConfigSpec spec = mock(VirtualDeviceConfigSpec.class);
    doReturn(vd).when(diskManager).findDataDisk(any(), anyInt());
    doReturn(spec).when(diskManager).createVirtualDeviceConfigSpec();
    doReturn((long) 2048).when(vd).getCapacityInKB();
    doReturn(des).when(vd).getDeviceInfo();
    doReturn("test").when(des).getLabel();

    // when
    diskManager.configureSystemDisk(vmConfigSpec, 3, vd);

    // then
    verify(spec, times(1)).setDevice(vd);
  }
}
