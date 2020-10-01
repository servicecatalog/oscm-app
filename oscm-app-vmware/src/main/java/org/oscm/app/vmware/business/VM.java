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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.CustomFieldDef;
import com.vmware.vim25.GuestInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachinePowerState;
import com.vmware.vim25.VirtualMachineRuntimeInfo;
import com.vmware.vim25.VirtualMachineSnapshotInfo;
import com.vmware.vim25.VirtualMachineSnapshotTree;
import com.vmware.vim25.VirtualMachineSummary;

public class VM extends Template {

  private static final Logger logger = LoggerFactory.getLogger(VM.class);

  private static final String GUEST_STATE_RUNNING = "running";
  private static final String TOOLS_RUNNING_STATE = "guestToolsRunning";

  private ManagedObjectReference vmInstance;
  private ManagedObjectReference customFieldsManager;
  private VirtualMachineConfigInfo configSpec;
  private ManagedObjectReference folder;
  private GuestInfo guestInfo;
  private String instanceName;
  private VirtualMachineSummary virtualMachineSummary;
  private VirtualMachineSnapshotInfo virtualMachineSnapshotInfo;

  public VM(VMwareClient vmw, String instanceName) throws Exception {
    this.vmw = vmw;
    this.instanceName = instanceName;
    vmInstance = vmw.getServiceUtil().getDecendentMoRef(null, "VirtualMachine", instanceName);
    customFieldsManager = vmw.getConnection().getServiceContent().getCustomFieldsManager();
    configSpec =
        (VirtualMachineConfigInfo) vmw.getServiceUtil().getDynamicProperty(vmInstance, "config");
    folder = (ManagedObjectReference) vmw.getServiceUtil().getDynamicProperty(vmInstance, "parent");
    guestInfo = (GuestInfo) vmw.getServiceUtil().getDynamicProperty(vmInstance, "guest");
    virtualMachineSummary =
        (VirtualMachineSummary) vmw.getServiceUtil().getDynamicProperty(vmInstance, "summary");
    virtualMachineSnapshotInfo =
        (VirtualMachineSnapshotInfo)
            vmw.getServiceUtil().getDynamicProperty(vmInstance, "snapshot");

    if (vmInstance == null || configSpec == null || folder == null || guestInfo == null) {
      logger.warn("failed to retrieve VM");
      throw new Exception(
          "VM " + instanceName + " does not exist or failed to retrieve information.");
    }
  }

  public String createVmUrl(VMPropertyHandler ph)
      throws InvalidStateFaultMsg, RuntimeFaultFaultMsg {

    StringBuilder url = new StringBuilder();
    url.append("https://");
    url.append(ph.getTargetVCenterServer());
    url.append(":");
    url.append(ph.getVsphereConsolePort());
    url.append("/vsphere-client/webconsole.html?vmId=");
    url.append(vmInstance.getValue().toString());
    url.append("&vmName=");
    url.append(configSpec.getName());
    url.append("&serverGuid=");
    url.append(vmw.getConnection().getServiceContent().getAbout().getInstanceUuid());
    url.append("&host=");
    url.append(ph.getTargetVCenterServer());
    url.append(":443");
    url.append("&sessionTicket=");
    url.append("cst-VCT");

    return url.toString();
  }

  public List<String> getSnashotsAsList() {
    List<String> snapshots = new ArrayList<String>();
    if (virtualMachineSnapshotInfo != null) {
      List<VirtualMachineSnapshotTree> snap = virtualMachineSnapshotInfo.getRootSnapshotList();
      snapshots.addAll(getSnapshots(snap, new ArrayList<String>(), ""));
    }
    return snapshots;
  }

  private List<String> getSnapshots(
      List<VirtualMachineSnapshotTree> vmst, ArrayList<String> snaps, String indent) {

    for (Iterator<VirtualMachineSnapshotTree> iterator = vmst.iterator(); iterator.hasNext(); ) {
      VirtualMachineSnapshotTree snap = iterator.next();
      snaps.add(indent + "Snapshot: " + snap.getName());
      getSnapshots(snap.getChildSnapshotList(), snaps, indent + " ");
    }
    return snaps;
  }

  public Integer getGuestMemoryUsage() {
    return virtualMachineSummary.getQuickStats().getGuestMemoryUsage();
  }

  public void setCostumValues(Map<String, String> settings) {
    List<CustomFieldDef> fields;
    try {
      fields =
          (List<CustomFieldDef>)
              vmw.getServiceUtil().getDynamicProperty(customFieldsManager, "field");

      for (CustomFieldDef field : fields) {
        if (settings.containsKey(field.getName())) {
          vmw.getConnection()
              .getService()
              .setCustomValue(vmInstance, field.getName(), settings.get(field.getName()));
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to set costum value for vm " + vmInstance, e);
    }
  }

  public Integer getOverallCpuUsage() {
    return virtualMachineSummary.getQuickStats().getOverallCpuUsage();
  }

  public Integer getUptimeSeconds() {
    return virtualMachineSummary.getQuickStats().getUptimeSeconds();
  }

  public String getStatus() {
    return guestInfo.getGuestState();
  }

  public String getGuestFullName() {
    return configSpec.getGuestFullName();
  }

  public boolean isLinux() {
    String guestid = configSpec.getGuestId();
    boolean isLinux =
        guestid.startsWith("cent")
            || guestid.startsWith("debian")
            || guestid.startsWith("freebsd")
            || guestid.startsWith("oracle")
            || guestid.startsWith("other24xLinux")
            || guestid.startsWith("other26xLinux")
            || guestid.startsWith("otherLinux")
            || guestid.startsWith("redhat")
            || guestid.startsWith("rhel")
            || guestid.startsWith("sles")
            || guestid.startsWith("suse")
            || guestid.startsWith("ubuntu");

    logger.debug(
        "instanceName: "
            + instanceName
            + " isLinux: "
            + isLinux
            + " guestid: "
            + configSpec.getGuestId()
            + " OS: "
            + configSpec.getGuestFullName());

    return isLinux;
  }

  public void updateServiceParameter(VMPropertyHandler paramHandler) throws Exception {
    logger.debug("instanceName: " + instanceName);
    int key = getDataDiskKey();
    if (key != -1) {
      paramHandler.setDataDiskKey(1, key);
    }

    if (!paramHandler.isServiceSettingTrue(VMPropertyHandler.TS_IMPORT_EXISTING_VM)
        && !paramHandler.getInstanceName().equals(guestInfo.getHostName())) {
      throw new Exception(
          "Instancename and hostname do not match. Hostname: "
              + guestInfo.getHostName()
              + "  Instancename: "
              + paramHandler.getInstanceName());
    }

    String targetFolder = (String) vmw.getServiceUtil().getDynamicProperty(folder, "name");

    Integer ramMB =
        (Integer)
            vmw.getServiceUtil().getDynamicProperty(vmInstance, "summary.config.memorySizeMB");
    paramHandler.setSetting(VMPropertyHandler.TS_AMOUNT_OF_RAM, ramMB.toString());
    paramHandler.setSetting(VMPropertyHandler.TS_NUMBER_OF_CPU, Integer.toString(getNumCPU()));
    paramHandler.setSetting(VMPropertyHandler.TS_TARGET_FOLDER, targetFolder);

    paramHandler.setSetting(VMPropertyHandler.TS_DISK_SIZE, getDiskSizeInGB(1));

    paramHandler.setSetting(
        VMPropertyHandler.TS_DATA_DISK_SIZE.replace("#", "1"), getDiskSizeInGB(2));
    paramHandler.setSetting(
        VMPropertyHandler.TS_NUMBER_OF_NICS, Integer.toString(getNumberOfNICs()));

    int i = 1;
    List<GuestNicInfo> nicList = guestInfo.getNet();
    for (GuestNicInfo info : nicList) {
      if (info.getIpAddress() != null && info.getIpAddress().size() > 0) {
        paramHandler.setSetting("NIC" + i + "_IP_ADDRESS", info.getIpAddress().get(0));
        if (info.getNetwork() != null) {
          paramHandler.setSetting("NIC" + i + "_NETWORK_ADAPTER", info.getNetwork());
        }
        i++;
      }
    }
  }

  public OS detectOs() {
    if (configSpec.getGuestId().startsWith("win")) {
      return OS.WINDOWS;
    }
    return OS.LINUX;
  }

  public boolean isRunning() throws Exception {
    VirtualMachineRuntimeInfo vmRuntimeInfo =
        (VirtualMachineRuntimeInfo) vmw.getServiceUtil().getDynamicProperty(vmInstance, "runtime");

    boolean isRunning = false;
    if (vmRuntimeInfo != null) {
      isRunning = !VirtualMachinePowerState.POWERED_OFF.equals(vmRuntimeInfo.getPowerState());
      logger.debug(Boolean.toString(isRunning));
    } else {
      logger.warn("Failed to retrieve runtime information from VM " + instanceName);
    }

    return isRunning;
  }

  public boolean isStopped() throws Exception {
    VirtualMachineRuntimeInfo vmRuntimeInfo =
        (VirtualMachineRuntimeInfo) vmw.getServiceUtil().getDynamicProperty(vmInstance, "runtime");

    if (vmRuntimeInfo != null) {
      return VirtualMachinePowerState.POWERED_OFF.equals(vmRuntimeInfo.getPowerState());
    }
    logger.warn("Failed to retrieve runtime information from VM " + instanceName);
    return false;
  }

  public TaskInfo start() throws Exception {
    logger.debug("instanceName: " + instanceName);
    ManagedObjectReference startTask =
        vmw.getConnection().getService().powerOnVMTask(vmInstance, null);

    TaskInfo tInfo = (TaskInfo) vmw.getServiceUtil().getDynamicProperty(startTask, "info");
    return tInfo;
  }

  public TaskInfo stop(boolean forceStop) throws Exception {
    logger.debug("instanceName: " + instanceName + " forceStop: " + forceStop);
    TaskInfo tInfo = null;

    if (forceStop) {
      logger.debug("Call vSphere API: powerOffVMTask() instanceName: " + instanceName);
      ManagedObjectReference stopTask = vmw.getConnection().getService().powerOffVMTask(vmInstance);
      tInfo = (TaskInfo) vmw.getServiceUtil().getDynamicProperty(stopTask, "info");
    } else {

      if (isRunning()) {
        logger.debug("Call vSphere API: shutdownGuest() instanceName: " + instanceName);
        vmw.getConnection().getService().shutdownGuest(vmInstance);
      }
    }

    return tInfo;
  }

  public ManagedObjectReference getFolder() {
    return folder;
  }

  public void runScript(VMPropertyHandler paramHandler) throws Exception {
    logger.debug("instanceName: " + instanceName);
    String scriptURL = paramHandler.getServiceSetting(VMPropertyHandler.TS_SCRIPT_URL);
    Script script = Script.getInstance();
    if (scriptURL != null) {
      try {
        script.initScript(paramHandler, detectOs());
        script.execute(vmw, vmInstance);
      } catch (Exception e) {
        script.setScriptExecuting(false);
        throw e;
      }
    }
  }

  public void updateLinuxVMPassword(VMPropertyHandler paramHandler) throws Exception {
    logger.debug("instanceName: " + instanceName);

    String password = paramHandler.getServiceSetting(VMPropertyHandler.TS_LINUX_ROOT_PWD);
    String updateScript = VMScript.updateLinuxVMRootPassword(password);
    Script script = Script.getInstance();
    if (updateScript != null) {
      script.initScript(paramHandler, detectOs(), updateScript);
      script.execute(vmw, vmInstance);
    }
  }

  public boolean isScriptExecuting() {
    Script script = Script.getInstance();
    return script.isScriptExecuting();
  }

  public int getNumberOfNICs() throws Exception {
    return NetworkManager.getNumberOfNICs(vmw, vmInstance);
  }

  public String getNetworkName(int numNic) throws Exception {
    return NetworkManager.getNetworkName(vmw, vmInstance, numNic);
  }

  /**
   * Reconfigures VMware instance. Memory, CPU, disk space and network adapter. The VM has been
   * created and must be stopped to reconfigure the hardware.
   */
  public TaskInfo reconfigureVirtualMachine(VMPropertyHandler paramHandler) throws Exception {
    logger.debug("instanceName: " + instanceName);

    VimPortType service = vmw.getConnection().getService();
    VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();

    vmConfigSpec.setMemoryMB(Long.valueOf(paramHandler.getConfigMemoryMB()));
    vmConfigSpec.setNumCPUs(Integer.valueOf(paramHandler.getConfigCPUs()));

    String reqUser = paramHandler.getServiceSetting(VMPropertyHandler.REQUESTING_USER);

    String comment =
        Messages.get(
            paramHandler.getLocale(),
            "vm_comment",
            new Object[] {
              paramHandler.getSettings().getOrganizationName(),
              paramHandler.getSettings().getSubscriptionId(),
              reqUser
            });
    String annotation = vmConfigSpec.getAnnotation();
    comment = updateComment(comment, annotation);
    vmConfigSpec.setAnnotation(comment);

    DiskManager diskManager = createDiskManager(paramHandler);
    diskManager.reconfigureDisks(vmConfigSpec, vmInstance);

    configureNetworkAdapter(paramHandler, vmConfigSpec);

    logger.debug("Call vSphere API: reconfigVMTask()");
    ManagedObjectReference reconfigureTask = service.reconfigVMTask(vmInstance, vmConfigSpec);

    return (TaskInfo) vmw.getServiceUtil().getDynamicProperty(reconfigureTask, "info");
  }

  protected void configureNetworkAdapter(
      VMPropertyHandler paramHandler, VirtualMachineConfigSpec vmConfigSpec) throws Exception {
    NetworkManager.configureNetworkAdapter(vmw, vmConfigSpec, paramHandler, vmInstance);
  }

  protected DiskManager createDiskManager(VMPropertyHandler paramHandler) {
    return new DiskManager(vmw, paramHandler);
  }

  public TaskInfo updateCommentField(String comment) throws Exception {
    logger.debug("instanceName: " + instanceName + " comment: " + comment);
    VimPortType service = vmw.getConnection().getService();
    VirtualMachineConfigSpec vmConfigSpec = new VirtualMachineConfigSpec();
    String annotation = vmConfigSpec.getAnnotation();
    comment = updateComment(comment, annotation);
    vmConfigSpec.setAnnotation(comment);
    logger.debug("Call vSphere API: reconfigVMTask()");
    ManagedObjectReference reconfigureTask = service.reconfigVMTask(vmInstance, vmConfigSpec);

    return (TaskInfo) vmw.getServiceUtil().getDynamicProperty(reconfigureTask, "info");
  }

  String updateComment(String comment, String annotation) {
    if (annotation == null) {
      annotation = "";
    }
    Pattern pattern =
        Pattern.compile(
            ".*" + "CT-MG \\{" + "[\\r\\n]+(.*?)[\\r\\n]+" + "\\}" + ".*", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(annotation);
    if (matcher.find()) {
      return annotation.replace(matcher.group(1), comment);
    }

    if (annotation.trim().length() == 0) {
      return "CT-MG {\n".concat(comment).concat("\n}");
    }
    return annotation.concat("\n").concat("CT-MG {\n").concat(comment).concat("\n}");
  }

  /**
   * Delete VMware instance on vSphere server.
   *
   * @param vmw connected VMware client entity
   * @param instanceId id of the instance
   */
  public TaskInfo delete() throws Exception {
    logger.debug("Call vSphere API: destroyTask() instanceName: " + instanceName);
    ManagedObjectReference startTask = vmw.getConnection().getService().destroyTask(vmInstance);

    return (TaskInfo) vmw.getServiceUtil().getDynamicProperty(startTask, "info");
  }

  boolean arePortgroupsAvailable(VMPropertyHandler properties) {
    int numberOfNICs =
        Integer.parseInt(properties.getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS));
    for (int i = 1; i <= numberOfNICs; i++) {
      if (!properties.getPortGroup(i).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  public VMwareGuestSystemStatus getState(VMPropertyHandler properties) throws Exception {

    boolean networkCardsConnected = areNetworkCardsConnected();
    boolean validHostname = isValidHostname();
    boolean validIp = isValidIp(properties);

    logger.debug(
        String.format(
            "getState networkCardsConnected=%s validHostname=%s, validIp=%s",
            String.valueOf(networkCardsConnected),
            String.valueOf(validHostname),
            String.valueOf(validIp)));

    if (isLinux()) {
      boolean firstStart =
          isNotEmpty(guestInfo.getHostName())
              && !validIp
              && guestIsReady()
              && networkCardsConnected;

      boolean secondStart = validHostname && validIp && guestIsReady() && networkCardsConnected;

      if (firstStart || secondStart) {
        logger.debug("firstStart: " + firstStart + " secondStart: " + secondStart);
        return VMwareGuestSystemStatus.GUEST_READY;
      }

      logger.debug(createLogForGetState(validHostname, properties, networkCardsConnected, validIp));
      return VMwareGuestSystemStatus.GUEST_NOTREADY;
    }

    if (validHostname
        && networkCardsConnected
        && (validIp || arePortgroupsAvailable(properties))
        && guestIsReady()) {
      return VMwareGuestSystemStatus.GUEST_READY;
    }

    logger.debug(createLogForGetState(validHostname, properties, networkCardsConnected, validIp));
    return VMwareGuestSystemStatus.GUEST_NOTREADY;
  }

  boolean guestIsReady() {
    return (isGuestSystemRunning() && areGuestToolsRunning());
  }

  String createLogForGetState(
      boolean validHostname,
      VMPropertyHandler configuration,
      boolean isConnected,
      boolean validIp) {

    StringBuilder sb = new StringBuilder();
    sb.append("Guest system is not ready yet ");
    sb.append("[");
    sb.append("hostname (" + validHostname + ") =" + guestInfo.getHostName() + ", ");
    sb.append("ipReady=" + validIp + ", ");
    for (int i = 1; i <= configuration.getNumberOfNetworkAdapter(); i++) {
      GuestNicInfo info = getNicInfo(configuration, i);
      if (info != null) {
        sb.append(info.getNetwork() + "=");
        sb.append(info.getIpAddress());
        sb.append(",");
      }
    }
    sb.append("guestState=" + guestInfo.getGuestState() + ", ");
    sb.append("toolsState=" + guestInfo.getToolsStatus() + ", ");
    sb.append("toolsRunning=" + guestInfo.getToolsRunningStatus() + ", ");
    sb.append("isConnected=" + isConnected);
    sb.append("]");
    String logStatement = sb.toString();
    return logStatement;
  }

  private boolean isNotEmpty(String validate) {
    return validate != null && validate.length() > 0;
  }

  boolean areGuestToolsRunning() {
    return TOOLS_RUNNING_STATE.equals(guestInfo.getToolsRunningStatus());
  }

  boolean isGuestSystemRunning() {
    return GUEST_STATE_RUNNING.equals(guestInfo.getGuestState());
  }

  boolean areNetworkCardsConnected() {
    boolean isConnected = false;
    if (guestInfo.getNet() != null && !guestInfo.getNet().isEmpty()) {
      isConnected = true;
    }
    for (GuestNicInfo nicInfo : guestInfo.getNet()) {
      isConnected = isConnected && nicInfo.isConnected();
    }
    return isConnected;
  }

  boolean isValidHostname() {
    String hostname = guestInfo.getHostName();
    return hostname != null
        && hostname.length() > 0
        && hostname.toUpperCase().startsWith(instanceName.toUpperCase());
  }

  boolean isValidIp(VMPropertyHandler configuration) {
    for (int i = 1; i <= configuration.getNumberOfNetworkAdapter(); i++) {
      GuestNicInfo info = getNicInfo(configuration, i);
      if (info == null) {
        return false;
      }

      if (configuration.isAdapterConfiguredManually(i)) {
        logger.debug(String.format("Manual configured IP %s", configuration.getIpAddress(i)));
        if (!containsIpAddress(info, configuration.getIpAddress(i))) {
          return false;
        }
      } else {
        if (!ipAddressExists(info)) {
          logger.debug(String.format("GuestInfo for network %s has no IPs", info.getNetwork()));
          return false;
        }
      }
    }

    return true;
  }

  GuestNicInfo getNicInfo(VMPropertyHandler configuration, int i) {
    if (configuration.getNetworkAdapter(i).isEmpty()) {
      logger.debug(String.format("No network adapter %s", i));
      return null;
    }

    logger.debug(String.format("NIC adapter %s", configuration.getNetworkAdapter(i)));

    for (GuestNicInfo info : guestInfo.getNet()) {
      boolean dhcp = configuration.isAdapterConfiguredByDhcp(i);
      logger.debug(
          String.format("GuestInfo IP %s dhcp=%s", info.getIpAddress(), String.valueOf(dhcp)));
      if (dhcp) {
        return info;
      }

      logger.debug(String.format("GuestNicInfo.getNetwork = %s", info.getNetwork()));
      if (configuration.getNetworkAdapter(i).equals(info.getNetwork())) {
        return info;
      }
    }
    return null;
  }

  boolean containsIpAddress(GuestNicInfo info, String address) {
    return info.getIpAddress().contains(address);
  }

  boolean guestInfoContainsNic(String adapter) {
    for (GuestNicInfo info : guestInfo.getNet()) {
      if (info.getNetwork().equals(adapter)) {
        return true;
      }
    }

    return false;
  }

  boolean ipAddressExists(GuestNicInfo info) {
    if (info.getIpAddress().isEmpty()) {
      return false;
    }

    for (String ip : info.getIpAddress()) {
      if (ip == null || ip.trim().length() == 0) {
        return false;
      }
    }

    return true;
  }

  public String generateAccessInfo(VMPropertyHandler paramHandler) throws Exception {

    VMwareAccessInfo accInfo = new VMwareAccessInfo(paramHandler);
    String accessInfo = accInfo.generateAccessInfo(guestInfo);
    logger.debug(
        "Generated access information for service instance '" + instanceName + "':\n" + accessInfo);
    return accessInfo;
  }

  protected int getDataDiskKey() throws Exception {
    List<VirtualDevice> devices = configSpec.getHardware().getDevice();
    int countDisks = 0;
    int key = -1;
    for (VirtualDevice vdInfo : devices) {
      if (vdInfo instanceof VirtualDisk) {
        countDisks++;
        if (countDisks == 2) {
          key = ((VirtualDisk) vdInfo).getKey();
          break;
        }
      }
    }

    return key;
  }

  protected String getDiskSizeInGB(int disk) throws Exception {
    String size = "";
    List<VirtualDevice> devices = configSpec.getHardware().getDevice();
    int countDisks = 0;
    for (VirtualDevice vdInfo : devices) {
      if (vdInfo instanceof VirtualDisk) {
        countDisks++;
        if (countDisks == disk) {
          long gigabyte = ((VirtualDisk) vdInfo).getCapacityInKB() / 1024 / 1024;
          size = Long.toString(gigabyte);
          break;
        }
      }
    }

    return size;
  }

  public String getTotalDiskSizeInMB() throws Exception {
    long megabyte = 0;
    List<VirtualDevice> devices = configSpec.getHardware().getDevice();
    for (VirtualDevice vdInfo : devices) {
      if (vdInfo instanceof VirtualDisk) {
        megabyte = megabyte + (((VirtualDisk) vdInfo).getCapacityInKB() / 1024);
      }
    }

    return Long.toString(megabyte);
  }

  public Integer getNumCPU() {
    return configSpec.getHardware().getNumCPU();
  }

  public Integer getCoresPerCPU() {
    return configSpec.getHardware().getNumCoresPerSocket();
  }

  public String getCPUModel(VMPropertyHandler paramHandler) throws Exception {
    String datacenter = paramHandler.getTargetDatacenter();
    ManagedObjectReference dataCenterRef =
        vmw.getServiceUtil().getDecendentMoRef(null, "Datacenter", datacenter);
    if (dataCenterRef == null) {
      logger.error("Datacenter not found. dataCenter: " + datacenter);
      throw new APPlatformException(
          Messages.get(
              paramHandler.getLocale(), "error_invalid_datacenter", new Object[] {datacenter}));
    }

    String hostName = paramHandler.getServiceSetting(VMPropertyHandler.TS_TARGET_HOST);
    ManagedObjectReference hostRef =
        vmw.getServiceUtil().getDecendentMoRef(dataCenterRef, "HostSystem", hostName);
    if (hostRef == null) {
      logger.error("Target host " + hostName + " not found");
      throw new APPlatformException(Messages.getAll("error_invalid_host", new Object[] {hostName}));
    }

    return (String) vmw.getServiceUtil().getDynamicProperty(hostRef, "summary.hardware.cpuModel");
  }

  /** @return fully qualified domain name */
  public String getFQDN() {
    // TODO do not remove method. Please implement, return FQDN
    return "";
  }
}
