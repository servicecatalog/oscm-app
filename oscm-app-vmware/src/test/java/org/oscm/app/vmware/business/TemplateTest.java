/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 16 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualHardware;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;

/** @author worf */
public class TemplateTest {

  @Mock VMPropertyHandler paramHandler;
  @Mock VMwareClient vmw;
  @Mock ManagedObjectAccessor moa;
  @Mock VirtualMachineConfigInfo configSpec;
  @Mock VimPortType vimPortType;
  @Mock ServiceConnection serviceConnection;
  @Mock ManagedObjectReference vmTpl;
  @Mock VirtualMachineRelocateSpec relocSpec;
  @Mock ProvisioningSettings settings;

  @Spy Template template = new Template();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    setUpMocks();
  }

  private void setUpMocks() {
    template.vmw = vmw;
    doReturn(moa).when(vmw).getServiceUtil();
    doReturn(serviceConnection).when(vmw).getConnection();
    doReturn(vimPortType).when(serviceConnection).getService();
    doReturn(settings).when(paramHandler).getSettings();
  }

  @Test
  public void cloneVM() throws Exception {

    // given
    String datacenter = "datacenter";
    String tmpl = "template";
    Long templateDiskSpace = (long) 1024;

    ManagedObjectReference vmDataCenter = mock(ManagedObjectReference.class);
    ManagedObjectReference moRefTargetFolder = mock(ManagedObjectReference.class);
    ManagedObjectReference cloneTask = mock(ManagedObjectReference.class);
    CustomizationSpecTemplate cst = mock(CustomizationSpecTemplate.class);
    CustomizationSpec cs = mock(CustomizationSpec.class);
    InventoryTemplate it = mock(InventoryTemplate.class);
    VirtualMachineCloneSpec vmcs = mock(VirtualMachineCloneSpec.class);
    VirtualMachineConfigSpec vmcls = mock(VirtualMachineConfigSpec.class);
    TaskInfo tInfo = mock(TaskInfo.class);

    doReturn(datacenter).when(paramHandler).getTargetDatacenter();
    doReturn("cluster").when(paramHandler).getTargetCluster();
    doReturn("template").when(paramHandler).getTemplateName();

    doReturn(vmDataCenter).when(template).getDataCenter(paramHandler, datacenter);
    doReturn(vmTpl).when(template).getVMTemplate(paramHandler, datacenter, tmpl, vmDataCenter);
    doReturn(vmTpl).when(template).getVMTemplate(paramHandler, datacenter, tmpl, vmDataCenter);
    doReturn(templateDiskSpace).when(template).getTemplateDiskSpace(paramHandler, tmpl, vmTpl);
    doReturn(cst).when(template).createCustomizationSpecTemplate(paramHandler);
    doReturn(it).when(template).createInventoryTemplate(paramHandler);
    doReturn(vmcs).when(template).createVirtualMachineCloneSpec(relocSpec);
    doReturn(moRefTargetFolder).when(template).getMoRefTargetFolder(paramHandler, vmTpl);
    doReturn(relocSpec).when(it).getHostAndStorageSpec(vmDataCenter);
    doReturn(vmcls).when(template).createVirtualMachineConfigSpec(paramHandler);
    doReturn(cloneTask).when(vimPortType).cloneVMTask(any(), any(), anyString(), any());

    doReturn(cs).when(cst).getCustomizationSpec(configSpec);
    doReturn(configSpec).when(moa).getDynamicProperty(vmTpl, "config");
    doReturn(tInfo).when(moa).getDynamicProperty(cloneTask, "info");
    doNothing().when(template).setDiskSpaceMB(any(), anyString(), any(), anyDouble());

    // when
    TaskInfo result = template.cloneVM(paramHandler);
    // then
    assertEquals(tInfo, result);
  }

  @Test
  public void setDiskSpaceMB_templateDiskSpace() throws APPlatformException {

    // given
    double tplDiskSpace = 10;
    String templateName = "";
    doReturn(.0).when(paramHandler).getConfigDiskSpaceMB();

    // when
    template.setDiskSpaceMB(paramHandler, templateName, configSpec, tplDiskSpace);

    // then
    verify(paramHandler).setTemplateDiskSpaceMB(10);
  }

  @Test
  public void setDiskSpaceMB_costumUserDiskSpace() throws APPlatformException {

    // given
    double tplDiskSpace = 0;
    String templateName = "";
    doReturn(10.0).when(paramHandler).getConfigDiskSpaceMB();
    doReturn(20.0).when(template).getRequestedDiskSpace(paramHandler, templateName, configSpec);

    // when
    template.setDiskSpaceMB(paramHandler, templateName, configSpec, tplDiskSpace);

    // then
    verify(paramHandler).setTemplateDiskSpaceMB(20);
  }

  @Test(expected = APPlatformException.class)
  public void getMoRefTargetFolder_exeption()
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException, Exception {
    // given

    // when
    template.getMoRefTargetFolder(paramHandler, vmTpl);
    // then
  }

  @Test
  public void getMoRefTargetFolder_fromTemplate()
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException, Exception {
    // given
    ManagedObjectReference moRefTargetFolder = mock(ManagedObjectReference.class);
    doReturn(moRefTargetFolder).when(moa).getDynamicProperty(vmTpl, "parent");

    // when
    ManagedObjectReference result = template.getMoRefTargetFolder(paramHandler, vmTpl);

    // then
    assertEquals(moRefTargetFolder, result);
  }

  @Test
  public void getMoRefTargetFolder_fromPropertyHandler()
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException, Exception {
    // given
    ManagedObjectReference moRefTargetFolder = mock(ManagedObjectReference.class);
    doReturn("folder").when(paramHandler).getTargetFolder();
    doReturn(moRefTargetFolder).when(moa).getDecendentMoRef(null, "Folder", "folder");

    // when
    ManagedObjectReference result = template.getMoRefTargetFolder(paramHandler, vmTpl);

    // then
    assertEquals(moRefTargetFolder, result);
  }

  @Test
  public void createVirtualMachineConfigSpec() {

    // given
    doReturn("command").when(template).createConfigComment(paramHandler);

    // when
    VirtualMachineConfigSpec result = template.createVirtualMachineConfigSpec(paramHandler);

    // then
    assertEquals("command", result.getAnnotation());
  }

  @Test
  public void createConfigComment() {

    // given
    doReturn("org").when(settings).getOrganizationName();
    doReturn("sub").when(settings).getSubscriptionId();
    // when

    String command = template.createConfigComment(paramHandler);

    // then
    assertTrue(command.contains("Organization: org"));
    assertTrue(command.contains("Subscription ID: sub"));
  }

  @Test
  public void createVirtualMachineCloneSpec() {

    // given

    // when
    VirtualMachineCloneSpec cloneSpec = template.createVirtualMachineCloneSpec(relocSpec);

    // then
    assertFalse(cloneSpec.isTemplate());
    assertFalse(cloneSpec.isPowerOn());
    assertEquals(relocSpec, cloneSpec.getLocation());
  }

  @Test
  public void getRequestedDiskSpace() throws Exception {

    // given
    VirtualHardware vh = mock(VirtualHardware.class);
    VirtualDisk vd = mock(VirtualDisk.class);
    VirtualDisk vd1 = mock(VirtualDisk.class);
    List<VirtualDevice> devices = new ArrayList<VirtualDevice>();
    devices.add(vd);
    devices.add(vd1);
    doReturn(2).when(vd).getKey();
    doReturn(1).when(vd1).getKey();
    doReturn(2).when(vd).getUnitNumber();
    doReturn(1).when(vd1).getUnitNumber();

    when(paramHandler.getConfigDiskSpaceMB()).thenReturn(10.0);
    when(configSpec.getHardware()).thenReturn(vh);
    when(configSpec.getName()).thenReturn("name");
    when(vh.getDevice()).thenReturn(devices);
    doReturn(1024l).when(template).getSystemDiskCapacity(configSpec, devices);

    // when
    double result = template.getRequestedDiskSpace(paramHandler, "", configSpec);
    // then

    assertEquals(10, result, 1);
  }

  @Test(expected = APPlatformException.class)
  public void getRequestedDiskSpace_exeption() throws Exception {

    // given
    VirtualHardware vh = mock(VirtualHardware.class);
    VirtualDisk vd = mock(VirtualDisk.class);
    VirtualDisk vd1 = mock(VirtualDisk.class);
    List<VirtualDevice> devices = new ArrayList<VirtualDevice>();
    devices.add(vd);
    devices.add(vd1);
    doReturn(2).when(vd).getKey();
    doReturn(1).when(vd1).getKey();
    doReturn(2).when(vd).getUnitNumber();
    doReturn(1).when(vd1).getUnitNumber();

    when(paramHandler.getConfigDiskSpaceMB()).thenReturn(10.0);
    when(configSpec.getHardware()).thenReturn(vh);
    when(configSpec.getName()).thenReturn("name");
    when(vh.getDevice()).thenReturn(devices);
    doReturn(102400l).when(template).getSystemDiskCapacity(configSpec, devices);

    // when
    double result = template.getRequestedDiskSpace(paramHandler, "", configSpec);
    // then

    assertEquals(10, result, 1);
  }

  @Test(expected = APPlatformException.class)
  public void getTemplateDiskSpace_exception() throws APPlatformException, Exception {

    // given
    doReturn(null).when(moa).getDecendentMoRef(null, "Folder", "folder");

    // when
    template.getTemplateDiskSpace(paramHandler, "", vmTpl);
    // then
  }

  @Test
  public void getTemplateDiskSpace() throws APPlatformException, Exception {

    // given
    doReturn(1024l).when(moa).getDynamicProperty(vmTpl, "summary.storage.unshared");

    // when
    long result = template.getTemplateDiskSpace(paramHandler, "", vmTpl);

    // then
    assertEquals(1024, result);
  }
}
