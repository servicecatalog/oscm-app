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

import java.text.DecimalFormat;
import java.util.List;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMwareValue.Unit;
import org.oscm.app.vmware.i18n.Messages;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VirtualDevice;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigInfo;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;

public class Template {

  private static final Logger logger = LoggerFactory.getLogger(Template.class);
  protected VMwareClient vmw;

  /**
   * Creates a new VMware instance based on a given template.
   *
   * @param vmw connected VMware client entity
   * @param paramHandler entity which holds all properties of the instance.
   * @return name of the created instance
   */
  public TaskInfo cloneVM(VMPropertyHandler paramHandler) throws Exception {
    logger.info("cloneVMFromTemplate() template: " + paramHandler.getTemplateName());

    String datacenter = paramHandler.getTargetDatacenter();
    String cluster = paramHandler.getTargetCluster();
    String template = paramHandler.getTemplateName();
    logger.debug("Datacenter: " + datacenter + " Cluster: " + cluster + " Template: " + template);

    ManagedObjectReference vmDataCenter = getDataCenter(paramHandler, datacenter);
    ManagedObjectReference vmTpl = getVMTemplate(paramHandler, datacenter, template, vmDataCenter);

    Long templateDiskSpace = getTemplateDiskSpace(paramHandler, template, vmTpl);
    double tplDiskSpace = VMwareValue.fromBytes(templateDiskSpace.longValue()).getValue(Unit.MB);

    VirtualMachineConfigInfo configSpec =
        (VirtualMachineConfigInfo) vmw.getServiceUtil().getDynamicProperty(vmTpl, "config");

    setDiskSpaceMB(paramHandler, template, configSpec, tplDiskSpace);

    CustomizationSpecTemplate customSpec = createCustomizationSpecTemplate(paramHandler);
    CustomizationSpec custSpec = customSpec.getCustomizationSpec(configSpec);

    InventoryTemplate inventory = createInventoryTemplate(paramHandler);
    VirtualMachineRelocateSpec relocSpec = inventory.getHostAndStorageSpec(vmDataCenter);
    VirtualMachineCloneSpec cloneSpec = createVirtualMachineCloneSpec(relocSpec);

    VirtualMachineConfigSpec vmConfSpec = createVirtualMachineConfigSpec(paramHandler);

    cloneSpec.setCustomization(custSpec);
    cloneSpec.setConfig(vmConfSpec);

    ManagedObjectReference moRefTargetFolder = getMoRefTargetFolder(paramHandler, vmTpl);

    String newInstanceName = paramHandler.getInstanceName();
    logger.debug(
        "Call vSphere API: cloneVMTask() instancename: "
            + newInstanceName
            + " targetfolder: "
            + paramHandler.getTargetFolder());
    VimPortType service = vmw.getConnection().getService();
    ManagedObjectReference cloneTask =
        service.cloneVMTask(vmTpl, moRefTargetFolder, newInstanceName, cloneSpec);

    return (TaskInfo) vmw.getServiceUtil().getDynamicProperty(cloneTask, "info");
  }

  protected InventoryTemplate createInventoryTemplate(VMPropertyHandler paramHandler) {
    return new InventoryTemplate(vmw, paramHandler);
  }

  protected CustomizationSpecTemplate createCustomizationSpecTemplate(
      VMPropertyHandler paramHandler) {
    return new CustomizationSpecTemplate(paramHandler);
  }

  protected void setDiskSpaceMB(
      VMPropertyHandler paramHandler,
      String template,
      VirtualMachineConfigInfo configSpec,
      double tplDiskSpace)
      throws APPlatformException {
    if (paramHandler.getConfigDiskSpaceMB() != .0) {
      double requestedDiskSpace = getRequestedDiskSpace(paramHandler, template, configSpec);
      paramHandler.setTemplateDiskSpaceMB(requestedDiskSpace);
    } else {
      logger.debug("Use template disk space. template: " + template);
      paramHandler.setTemplateDiskSpaceMB(tplDiskSpace);
    }
  }

  protected ManagedObjectReference getMoRefTargetFolder(
      VMPropertyHandler paramHandler, ManagedObjectReference vmTpl)
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, Exception, APPlatformException {
    ManagedObjectReference moRefTargetFolder = null;
    String targetFolder = paramHandler.getTargetFolder();
    if (targetFolder != null) {
      moRefTargetFolder = vmw.getServiceUtil().getDecendentMoRef(null, "Folder", targetFolder);
    } else {
      moRefTargetFolder =
          (ManagedObjectReference) vmw.getServiceUtil().getDynamicProperty(vmTpl, "parent");
    }

    if (moRefTargetFolder == null) {
      logger.error("Target folder " + targetFolder + " not found.");
      throw new APPlatformException(
          Messages.get(
              paramHandler.getLocale(),
              "error_invalid_target_folder",
              new Object[] {targetFolder}));
    }
    return moRefTargetFolder;
  }

  protected VirtualMachineConfigSpec createVirtualMachineConfigSpec(
      VMPropertyHandler paramHandler) {
    VirtualMachineConfigSpec vmConfSpec = new VirtualMachineConfigSpec();
    String comment = createConfigComment(paramHandler);
    vmConfSpec.setAnnotation(comment);
    return vmConfSpec;
  }

  protected String createConfigComment(VMPropertyHandler paramHandler) {
    String respPerson = paramHandler.getServiceSetting(VMPropertyHandler.TS_RESPONSIBLE_PERSON);
    String reqUser = paramHandler.getServiceSetting(VMPropertyHandler.REQUESTING_USER);
    String systemvariante = "";
    String comment =
        Messages.get(
            paramHandler.getLocale(),
            "vm_comment",
            new Object[] {
              paramHandler.getSettings().getOrganizationName(),
              paramHandler.getSettings().getSubscriptionId(),
              reqUser,
              respPerson,
              systemvariante
            });
    return comment;
  }

  protected VirtualMachineCloneSpec createVirtualMachineCloneSpec(
      VirtualMachineRelocateSpec relocSpec) {
    VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
    cloneSpec.setLocation(relocSpec);
    cloneSpec.setPowerOn(false);
    cloneSpec.setTemplate(false);
    return cloneSpec;
  }

  protected double getRequestedDiskSpace(
      VMPropertyHandler paramHandler, String template, VirtualMachineConfigInfo configSpec)
      throws APPlatformException {
    double requestedDiskSpace = paramHandler.getConfigDiskSpaceMB();
    List<VirtualDevice> devices = configSpec.getHardware().getDevice();
    long capacityInKB = getSystemDiskCapacity(configSpec, devices);
    double requestedDiskSpaceKB = requestedDiskSpace * 1024.0;
    logger.debug(
        "Requested disk space: "
            + requestedDiskSpaceKB
            + "Template disk space: "
            + capacityInKB
            + " template: "
            + template);
    if (requestedDiskSpaceKB < capacityInKB) {
      String minValExp =
          new DecimalFormat("#0.#")
              .format(VMwareValue.fromMegaBytes(capacityInKB / 1024.0).getValue(Unit.GB));
      logger.error(
          "Requested disk space is smaller than template disk space. template: " + template);
      throw new APPlatformException(
          Messages.get(
              paramHandler.getLocale(), "error_invalid_diskspace", new Object[] {minValExp}));
    }
    return requestedDiskSpace;
  }

  protected long getSystemDiskCapacity(
      VirtualMachineConfigInfo configSpec, List<VirtualDevice> devices) throws APPlatformException {
    return DiskManager.getSystemDiskCapacity(devices, configSpec.getName());
  }

  protected Long getTemplateDiskSpace(
      VMPropertyHandler paramHandler, String template, ManagedObjectReference vmTpl)
      throws Exception, APPlatformException {
    Long templateDiskSpace =
        (Long) vmw.getServiceUtil().getDynamicProperty(vmTpl, "summary.storage.unshared");
    if (templateDiskSpace == null) {
      logger.error("Missing disk size in template. template: " + template);
      throw new APPlatformException(
          Messages.get(paramHandler.getLocale(), "error_missing_template_size"));
    }
    return templateDiskSpace;
  }

  protected ManagedObjectReference getVMTemplate(
      VMPropertyHandler paramHandler,
      String datacenter,
      String template,
      ManagedObjectReference vmDataCenter)
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    ManagedObjectReference vmTpl =
        vmw.getServiceUtil().getDecendentMoRef(vmDataCenter, "VirtualMachine", template);
    if (vmTpl == null) {
      logger.error(
          "Template not found in datacenter. datacenter: " + datacenter + " template: " + template);
      throw new APPlatformException(
          Messages.get(
              paramHandler.getLocale(), "error_invalid_template", new Object[] {template}));
    }
    return vmTpl;
  }

  protected ManagedObjectReference getDataCenter(VMPropertyHandler paramHandler, String datacenter)
      throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg, APPlatformException {
    ManagedObjectReference vmDataCenter =
        vmw.getServiceUtil().getDecendentMoRef(null, "Datacenter", datacenter);
    if (vmDataCenter == null) {
      logger.error("Datacenter not found. dataCenter: " + datacenter);
      throw new APPlatformException(
          Messages.get(
              paramHandler.getLocale(), "error_invalid_datacenter", new Object[] {datacenter}));
    }
    return vmDataCenter;
  }
}
