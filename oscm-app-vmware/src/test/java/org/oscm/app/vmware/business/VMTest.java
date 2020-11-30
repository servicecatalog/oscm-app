/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 30 Sep 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import com.vmware.vim25.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({VM.class, Script.class, VMScript.class})
public class VMTest {

  private VM vm;
  private ManagedObjectAccessor moa;
  private VMPropertyHandler ph;
  private ManagedObjectReference vmInstance;
  private ManagedObjectReference customFieldsManager;
  private ServiceConnection con;
  private ServiceContent cont;
  private VirtualMachineConfigInfo configSpec;
  private ManagedObjectReference folder;
  private GuestInfo guestInfo;
  private VirtualMachineSummary virtualMachineSummary;
  private VirtualMachineSnapshotInfo virtualMachineSnapshotInfo;
  private Script script;
  private AboutInfo aboutInfo;

  private VimPortType service;

  VMwareClient vmc;

  @Before
  public void before() throws Exception {
    vmc = mock(VMwareClient.class);
    moa = mock(ManagedObjectAccessor.class);
    ph = mock(VMPropertyHandler.class);
    vmInstance = mock(ManagedObjectReference.class);
    customFieldsManager = mock(ManagedObjectReference.class);
    con = mock(ServiceConnection.class);
    cont = mock(ServiceContent.class);
    configSpec = mock(VirtualMachineConfigInfo.class);
    folder = mock(ManagedObjectReference.class);
    guestInfo = mock(GuestInfo.class);
    service = mock(VimPortType.class);
    virtualMachineSummary = mock(VirtualMachineSummary.class);
    virtualMachineSnapshotInfo = mock(VirtualMachineSnapshotInfo.class);
    script = mock(Script.class);
    aboutInfo = mock(AboutInfo.class);
    PowerMockito.mockStatic(Script.class);
    PowerMockito.mockStatic(VMScript.class);

    init();
    vm = spy(new VM(vmc, "test"));
  }

  private void init() throws Exception {
    doReturn(moa).when(vmc).getServiceUtil();
    doReturn(configSpec).when(moa).getDynamicProperty(vmInstance, "config");
    doReturn(folder).when(moa).getDynamicProperty(vmInstance, "parent");
    doReturn(guestInfo).when(moa).getDynamicProperty(vmInstance, "guest");
    doReturn(vmInstance).when(moa).getDecendentMoRef(null, "VirtualMachine", "test");
    doReturn(con).when(vmc).getConnection();
    doReturn(cont).when(con).getServiceContent();
    doReturn(customFieldsManager).when(cont).getCustomFieldsManager();
    doReturn(service).when(con).getService();
    doReturn(virtualMachineSummary).when(moa).getDynamicProperty(vmInstance, "summary");
    doReturn(virtualMachineSnapshotInfo).when(moa).getDynamicProperty(vmInstance, "snapshot");
  }

  @Test
  public void setCostumValue() throws Exception {
    // given
    Map<String, String> settings =
        Stream.of(
            new String[][]{
                {"BACKUP", "true"},
                {"IP", "127.0.0.1"},
            })
            .collect(Collectors.toMap(data -> data[0], data -> data[1]));

    List<CustomFieldDef> fields = new ArrayList<CustomFieldDef>();
    CustomFieldDef def = new CustomFieldDef();
    def.setName("BACKUP");
    fields.add(def);

    doReturn(fields).when(moa).getDynamicProperty(customFieldsManager, "field");
    doNothing().when(service).setCustomValue(any(), any(), any());

    // when
    vm.setCostumValues(settings);
    // then

    verify(service, times(1)).setCustomValue(any(), any(), any());
  }

  @Test
  public void getSnashotsAsList() {

    // given
    List<VirtualMachineSnapshotTree> snaps = new ArrayList<VirtualMachineSnapshotTree>();
    VirtualMachineSnapshotTree vmst = new VirtualMachineSnapshotTree();
    vmst.setName("test");
    VirtualMachineSnapshotTree vmst1 = new VirtualMachineSnapshotTree();
    vmst1.setName("test test");
    snaps.add(vmst);
    snaps.add(vmst1);
    doReturn(snaps).when(virtualMachineSnapshotInfo).getRootSnapshotList();

    // when
    List<String> result = vm.getSnashotsAsList();
    // then
    assertEquals("Snapshot: test", result.get(0));
  }

  @Test
  public void isLinux_true() {
    // given
    doReturn("cent").when(configSpec).getGuestId();
    // when

    boolean result = vm.isLinux();
    // then

    assertTrue(result);
  }

  @Test
  public void isLinux_false() {
    // given
    doReturn("test").when(configSpec).getGuestId();
    // when

    boolean result = vm.isLinux();
    // then

    assertFalse(result);
  }

  @Test(expected = Exception.class)
  public void updateServiceParameter_Exception() throws Exception {
    // given
    doReturn(1).when(vm).getDataDiskKey();
    doReturn("test").when(ph).getInstanceName();
    doReturn("test1").when(guestInfo).getHostName();

    // when
    vm.updateServiceParameter(ph);
  }

  @Test
  public void updateServiceParameter() throws Exception {
    // given
    List<GuestNicInfo> nicList = new ArrayList<GuestNicInfo>();
    List<String> ip = new ArrayList<String>();
    ip.add("127.0.0.1");
    GuestNicInfo info = mock(GuestNicInfo.class);
    info.setNetwork("test");
    nicList.add(info);
    doReturn(ip).when(info).getIpAddress();
    doReturn("test").when(info).getNetwork();

    doReturn(1).when(vm).getNumberOfNICs();
    doReturn(1).when(vm).getDataDiskKey();
    doReturn(1).when(vm).getNumCPU();
    doReturn("4").when(vm).getDiskSizeInGB(anyInt());
    doReturn("test").when(ph).getInstanceName();
    doReturn("test").when(guestInfo).getHostName();
    doReturn(1024).when(moa).getDynamicProperty(vmInstance, "summary.config.memorySizeMB");
    doReturn(nicList).when(guestInfo).getNet();

    // when
    vm.updateServiceParameter(ph);

    // then
    verify(ph, times(8)).setSetting(anyString(), anyString());
  }

  @Test
  public void detectOs_windows() {
    // given
    doReturn("win").when(configSpec).getGuestId();

    // when
    OS os = vm.detectOs();

    // then
    assertEquals(OS.WINDOWS, os);
  }

  @Test
  public void detectOs_linux() {
    // given
    doReturn("tux").when(configSpec).getGuestId();

    // when
    OS os = vm.detectOs();

    // then
    assertEquals(OS.LINUX, os);
  }

  @Test
  public void isRunning_false() throws Exception {
    // given

    // when
    boolean result = vm.isRunning();
    // then

    assertFalse(result);
  }

  @Test
  public void isRunning_true() throws Exception {
    // given

    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);
    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmInstance, "runtime");
    doReturn(VirtualMachinePowerState.POWERED_ON).when(vmRuntimeInfo).getPowerState();

    // when
    boolean result = vm.isRunning();
    // then

    assertTrue(result);
  }

  @Test
  public void isStopped_false() throws Exception {
    // given

    // when
    boolean result = vm.isStopped();
    // then

    assertFalse(result);
  }

  @Test
  public void isStopped_true() throws Exception {
    // given

    VirtualMachineRuntimeInfo vmRuntimeInfo = mock(VirtualMachineRuntimeInfo.class);
    doReturn(vmRuntimeInfo).when(moa).getDynamicProperty(vmInstance, "runtime");
    doReturn(VirtualMachinePowerState.POWERED_OFF).when(vmRuntimeInfo).getPowerState();

    // when
    boolean result = vm.isStopped();
    // then

    assertTrue(result);
  }

  @Test
  public void start() throws Exception {
    // given
    TaskInfo result = new TaskInfo();
    ManagedObjectReference task = mock(ManagedObjectReference.class);
    doReturn(task).when(service).powerOnVMTask(anyObject(), anyObject());
    doReturn(result).when(moa).getDynamicProperty(task, "info");
    // when
    TaskInfo expected = vm.start();
    // then

    assertEquals(expected, result);
  }

  @Test
  public void stop() throws Exception {
    // given
    doReturn(true).when(vm).isRunning();

    // when
    vm.stop(false);

    // then
    verify(service, times(1)).shutdownGuest(anyObject());
  }

  @Test
  public void stop_force() throws Exception {
    // given
    TaskInfo result = new TaskInfo();
    ManagedObjectReference task = mock(ManagedObjectReference.class);
    doReturn(task).when(service).powerOffVMTask(anyObject());
    doReturn(result).when(moa).getDynamicProperty(task, "info");
    doReturn(true).when(vm).isRunning();

    // when
    TaskInfo expected = vm.stop(true);

    // then
    assertEquals(expected, result);
  }

  @Test
  public void reconfigureVirtualMachine() throws Exception {
    // given
    ProvisioningSettings settings = mock(ProvisioningSettings.class);
    DiskManager manager = mock(DiskManager.class);
    doReturn(settings).when(ph).getSettings();
    doReturn(manager).when(vm).createDiskManager(ph);
    doNothing().when(vm).configureNetworkAdapter(any(), any());
    doReturn("test").when(settings).getOrganizationName();
    doReturn("test").when(settings).getSubscriptionId();

    // when
    vm.reconfigureVirtualMachine(ph);

    // then
    verify(service, times(1)).reconfigVMTask(anyObject(), anyObject());
    verify(moa, times(6)).getDynamicProperty(anyObject(), anyString());
  }

  @Test
  public void updateCommentField() throws Exception {
    // given

    // when
    vm.updateCommentField("");
    // then
    verify(service, times(1)).reconfigVMTask(anyObject(), anyObject());
  }

  @Test
  public void updateComment() throws Exception {
    // given
    String expected = "testannotation\n" + "CT-MG {\n" + "testcommend\n" + "}";
    // when
    String result = vm.updateComment("testcommend", "testannotation");
    // then
    assertEquals(expected, result);
  }

  @Test
  public void delete() throws Exception {
    // given
    TaskInfo result = new TaskInfo();
    ManagedObjectReference task = mock(ManagedObjectReference.class);
    doReturn(task).when(service).destroyTask(anyObject());
    doReturn(result).when(moa).getDynamicProperty(task, "info");

    // when
    TaskInfo expected = vm.delete();

    // then
    assertEquals(expected, result);
  }

  @Test
  public void arePortgroupsAvailable() {
    // given
    doReturn("1").when(ph).getServiceSetting(any());
    doReturn("").when(ph).getPortGroup(1);

    // when
    boolean result = vm.arePortgroupsAvailable(ph);

    // then
    assertFalse(result);
  }

  @Test
  public void getState_notReaedy() throws Exception {

    // given
    doReturn(true).when(vm).areNetworkCardsConnected();
    doReturn(true).when(vm).isValidHostname();
    doReturn(true).when(vm).isValidIp(ph);
    doReturn(true).when(vm).isLinux();

    // when
    VMwareGuestSystemStatus state = vm.getState(ph);

    // then
    assertEquals(VMwareGuestSystemStatus.GUEST_NOTREADY, state);
  }

  @Test
  public void getState_reaedy() throws Exception {

    // given
    doReturn(true).when(vm).areNetworkCardsConnected();
    doReturn(true).when(vm).isValidHostname();
    doReturn(true).when(vm).isValidIp(ph);
    doReturn(true).when(vm).isLinux();
    doReturn(true).when(vm).guestIsReady();
    doReturn("name").when(guestInfo).getHostName();

    // when
    VMwareGuestSystemStatus state = vm.getState(ph);

    // then
    assertEquals(VMwareGuestSystemStatus.GUEST_READY, state);
  }

  @Test
  public void createLogForGetState() {

    // given
    String expected =
        "Guest system is not ready yet [hostname (true) =null, ipReady=true, guestState=null, toolsState=null, toolsRunning=null, isConnected=true]";

    // when
    String result = vm.createLogForGetState(true, ph, true, true);

    // then
    assertEquals(expected, result);
  }

  @Test
  public void areNetworkCardsConnected() {
    // given
    List<GuestNicInfo> nicList = new ArrayList<GuestNicInfo>();
    List<String> ip = new ArrayList<String>();
    ip.add("127.0.0.1");
    GuestNicInfo info = mock(GuestNicInfo.class);
    info.setNetwork("test");
    nicList.add(info);
    doReturn(ip).when(info).getIpAddress();
    doReturn("test").when(info).getNetwork();

    doReturn(nicList).when(guestInfo).getNet();
    // when
    boolean result = vm.areNetworkCardsConnected();

    assertFalse(result);
  }

  @Test
  public void isValidHostname() {
    // given
    doReturn("Test").when(guestInfo).getHostName();
    // when
    boolean result = vm.isValidHostname();

    // then
    assertTrue(result);
  }

  @Test
  public void isValidIp() {
    // given
    List<String> ip = new ArrayList<String>();
    ip.add("127.0.0.1");
    GuestNicInfo info = mock(GuestNicInfo.class);
    info.setNetwork("test");
    doReturn(ip).when(info).getIpAddress();
    doReturn("test").when(info).getNetwork();
    doReturn(1).when(ph).getNumberOfNetworkAdapter();
    doReturn(info).when(vm).getNicInfo(ph, 1);

    // when
    boolean result = vm.isValidIp(ph);

    assertTrue(result);
  }

  @Test
  public void getNicInfo() {
    // given
    List<GuestNicInfo> nicList = new ArrayList<GuestNicInfo>();
    List<String> ip = new ArrayList<String>();
    ip.add("127.0.0.1");
    GuestNicInfo info = mock(GuestNicInfo.class);
    info.setNetwork("test");
    nicList.add(info);
    doReturn(ip).when(info).getIpAddress();
    doReturn("test").when(info).getNetwork();
    doReturn(nicList).when(guestInfo).getNet();
    doReturn("test").when(info).getNetwork();
    doReturn("test").when(ph).getNetworkAdapter(1);
    doReturn(nicList).when(guestInfo).getNet();

    // when
    GuestNicInfo result = vm.getNicInfo(ph, 1);

    // then
    assertEquals(info, result);
  }

  @Test
  public void getDataDiskKey() throws Exception {
    // given
    VirtualHardware vh = mock(VirtualHardware.class);
    List<VirtualDevice> vds = new ArrayList<VirtualDevice>();
    VirtualDisk vd = mock(VirtualDisk.class);
    VirtualDisk vd1 = mock(VirtualDisk.class);
    vds.add(vd);
    vds.add(vd1);
    doReturn(vh).when(configSpec).getHardware();
    doReturn(vds).when(vh).getDevice();
    doReturn(2).when(vd1).getKey();

    // when
    int result = vm.getDataDiskKey();
    // then

    assertEquals(2, result);
  }

  @Test
  public void getDiskSizeInGB() throws Exception {
    // given
    VirtualHardware vh = mock(VirtualHardware.class);
    List<VirtualDevice> vds = new ArrayList<VirtualDevice>();
    VirtualDisk vd = mock(VirtualDisk.class);
    VirtualDisk vd1 = mock(VirtualDisk.class);
    vds.add(vd);
    vds.add(vd1);
    doReturn(vh).when(configSpec).getHardware();
    doReturn(vds).when(vh).getDevice();
    doReturn(2097152L).when(vd1).getCapacityInKB();

    // when
    String result = vm.getDiskSizeInGB(2);
    // then

    assertEquals("2", result);
  }

  @Test
  public void getTotalDiskSizeInMB() throws Exception {
    // given
    VirtualHardware vh = mock(VirtualHardware.class);
    List<VirtualDevice> vds = new ArrayList<VirtualDevice>();
    VirtualDisk vd = mock(VirtualDisk.class);
    VirtualDisk vd1 = mock(VirtualDisk.class);
    vds.add(vd);
    vds.add(vd1);
    doReturn(vh).when(configSpec).getHardware();
    doReturn(vds).when(vh).getDevice();
    doReturn(2097152L).when(vd).getCapacityInKB();
    doReturn(2097152L).when(vd1).getCapacityInKB();

    // when
    String result = vm.getTotalDiskSizeInMB();
    // then

    assertEquals("4096", result);
  }

  @Test(expected = Exception.class)
  public void getCPUModel_exception() throws Exception {

    // given

    // when
    vm.getCPUModel(ph);

    // then
  }

  @Test(expected = APPlatformException.class)
  public void getCPUModel_Exception2() throws Exception {

    // given
    ManagedObjectReference dataCenterRef = mock(ManagedObjectReference.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    doReturn("test").when(ph).getTargetDatacenter();
    doReturn("test").when(ph).getServiceSetting(any());
    doReturn(dataCenterRef).when(moa).getDecendentMoRef(null, "Datacenter", "test");
    doReturn(hostRef).when(moa).getDecendentMoRef(null, "HostSystem", "test");
    doReturn("true").when(moa).getDynamicProperty(hostRef, "summary.hardware.cpuModel");

    // when
    vm.getCPUModel(ph);

    // then
  }

  @Test
  public void getCPUModel() throws Exception {

    // given
    ManagedObjectReference dataCenterRef = mock(ManagedObjectReference.class);
    ManagedObjectReference hostRef = mock(ManagedObjectReference.class);
    doReturn("test").when(ph).getTargetDatacenter();
    doReturn("test").when(ph).getServiceSetting(any());
    doReturn(dataCenterRef).when(moa).getDecendentMoRef(null, "Datacenter", "test");
    doReturn(hostRef).when(moa).getDecendentMoRef(dataCenterRef, "HostSystem", "test");
    doReturn("true").when(moa).getDynamicProperty(hostRef, "summary.hardware.cpuModel");

    // when
    vm.getCPUModel(ph);

    // then
    assertEquals("true", "true");
  }

  @Test
  public void testRunScript() throws Exception {
    // given
    when(ph.getServiceSetting(anyString())).thenReturn("https://company.com/script");
    PowerMockito.when(Script.getInstance()).thenReturn(script);
    when(configSpec.getGuestId()).thenReturn("windows");
    // when
    vm.runScript(ph);
    // then
    verify(script, times(1)).execute(any(), any());
    verify(script, never()).setScriptExecuting(false);
  }

  @Test(expected = Exception.class)
  public void testRunScriptThrowException() throws Exception {
    // given
    when(ph.getServiceSetting(anyString())).thenReturn("https://company.com/script");
    // when
    vm.runScript(ph);
  }

  @Test
  public void testUpdateLinuxVMPassword() throws Exception {
    // given
    when(ph.getServiceSetting(anyString())).thenReturn("password");
    PowerMockito.when(VMScript.updateLinuxVMRootPassword("password")).thenReturn("update");
    PowerMockito.when(Script.getInstance()).thenReturn(script);
    when(configSpec.getGuestId()).thenReturn("windows");
    // when
    vm.updateLinuxVMPassword(ph);
    // then
    verify(script, times(1)).execute(any(), any());
  }

  @Test
  public void testCreateVmUrl() throws Exception {
    //given
    when(ph.getTargetVCenterServer()).thenReturn("oscm-app");
    when(ph.getVsphereConsolePort()).thenReturn("8080");
    when(vmInstance.getValue()).thenReturn("14000");
    when(cont.getAbout()).thenReturn(aboutInfo);
    // when
    String result = vm.createVmUrl(ph);
    // then
    assertEquals("https://oscm-app:8080/vsphere-client/webconsole.html?vmId=14000&vmName=null&serverGuid=null&host=oscm-app:443&sessionTicket=cst-VCT", result);
  }
}
