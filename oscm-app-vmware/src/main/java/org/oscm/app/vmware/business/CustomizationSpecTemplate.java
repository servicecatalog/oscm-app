/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 15 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.CustomizationAdapterMapping;
import com.vmware.vim25.CustomizationDhcpIpGenerator;
import com.vmware.vim25.CustomizationFixedIp;
import com.vmware.vim25.CustomizationGlobalIPSettings;
import com.vmware.vim25.CustomizationGuiRunOnce;
import com.vmware.vim25.CustomizationGuiUnattended;
import com.vmware.vim25.CustomizationIPSettings;
import com.vmware.vim25.CustomizationIdentification;
import com.vmware.vim25.CustomizationLinuxOptions;
import com.vmware.vim25.CustomizationLinuxPrep;
import com.vmware.vim25.CustomizationPassword;
import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.CustomizationSysprep;
import com.vmware.vim25.CustomizationUserData;
import com.vmware.vim25.CustomizationVirtualMachineName;
import com.vmware.vim25.CustomizationWinOptions;
import com.vmware.vim25.VirtualMachineConfigInfo;

/** @author worf */
public class CustomizationSpecTemplate {

  private static final int DEFAULT_TIMEZONE = 110;

  VMPropertyHandler paramHandler;

  private static final Logger logger = LoggerFactory.getLogger(CustomizationSpecTemplate.class);

  public CustomizationSpecTemplate(VMPropertyHandler paramHandler) {
    this.paramHandler = paramHandler;
  }

  /**
   * Generates customization specification for OS specific deployment.
   *
   * @param custProps customization specific parameters
   * @return filled VMware customization block
   */
  protected CustomizationSpec getCustomizationSpec(VirtualMachineConfigInfo configSpec)
      throws APPlatformException {

    String guestid = getGuestId(configSpec);

    boolean isLinux = isLinux(guestid);
    boolean isWindows = isWindows(guestid);

    logger.debug(
        "isLinux: "
            + isLinux
            + " isWindows: "
            + isWindows
            + " guestid: "
            + configSpec.getGuestId()
            + " OS: "
            + configSpec.getGuestFullName());

    validateOsIsLinuxOrWindows(configSpec, isLinux, isWindows);

    CustomizationSpec cspec = createCustomizationSpec();

    if (isLinux) {
      configureLinuxSpec(cspec);
    }
    if (isWindows) {
      configureWindowsSpec(cspec);
    }

    setIpsForCustomSpec(isWindows, cspec);
    return cspec;
  }

  protected void setIpsForCustomSpec(boolean isWindows, CustomizationSpec cspec) {
    int numberOfNICs =
        Integer.parseInt(paramHandler.getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS));
    logger.debug("Number of NICs in template: " + numberOfNICs);
    for (int i = 1; i <= numberOfNICs; i++) {
      setIpForNetworkCard(isWindows, cspec, i);
    }
  }

  protected void setIpForNetworkCard(boolean isWindows, CustomizationSpec cspec, int i) {
    CustomizationAdapterMapping networkAdapter = new CustomizationAdapterMapping();
    CustomizationIPSettings ipSettings = new CustomizationIPSettings();
    if (paramHandler.isAdapterConfiguredByDhcp(i)) {
      CustomizationDhcpIpGenerator publicDhcpIp = new CustomizationDhcpIpGenerator();
      ipSettings.setIp(publicDhcpIp);
    } else if (paramHandler.isAdapterConfiguredByPortgroupIPPool(i)) {
      configureByPortgroupIPPool(isWindows, i, ipSettings);
    } else {
      configureManually(isWindows, i, ipSettings);
    }
    networkAdapter.setAdapter(ipSettings);
    cspec.getNicSettingMap().add(networkAdapter);
  }

  protected void configureWindowsSpec(CustomizationSpec cspec) throws APPlatformException {
    CustomizationSysprep sprep = new CustomizationSysprep();

    if (paramHandler.isServiceSettingTrue(VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN)) {
      addVmToWindowsDomain(sprep);
    } else {
      addVmToWindowsWorkgroup(sprep);
    }
    CustomizationGuiUnattended guiUnattended = createCustomizationGuiUnattended();
    CustomizationUserData userData = createVmUserData();
    CustomizationWinOptions options = createCustomizationWinOptions();
    setSysprepCommand(sprep);

    sprep.setGuiUnattended(guiUnattended);
    sprep.setUserData(userData);
    cspec.setIdentity(sprep);
    cspec.setOptions(options);
  }

  protected CustomizationWinOptions createCustomizationWinOptions() {
    CustomizationWinOptions options = new CustomizationWinOptions();
    options.setChangeSID(true);
    options.setDeleteAccounts(false);
    return options;
  }

  protected CustomizationUserData createVmUserData() {
    CustomizationUserData userData = new CustomizationUserData();
    userData.setComputerName(new CustomizationVirtualMachineName());

    String fullname = paramHandler.getResponsibleUserAsString(paramHandler.getLocale());
    if (fullname == null) {
      fullname = "No responsible user defined";
    }
    logger.debug("CustomizationUserData.fullName: " + fullname);

    userData.setFullName(fullname);
    userData.setOrgName("Created by OSCM");

    String licenseKey = paramHandler.getServiceSetting(VMPropertyHandler.TS_WINDOWS_LICENSE_KEY);

    if (licenseKey != null && licenseKey.trim().length() > 0) {
      userData.setProductId(licenseKey);
    } else {
      userData.setProductId("");
    }

    return userData;
  }

  protected void setSysprepCommand(CustomizationSysprep sprep) {
    String command = paramHandler.getServiceSetting(VMPropertyHandler.TS_SYSPREP_RUNONCE_COMMAND);

    if (command != null) {
      logger.debug("sysprep runonce command: " + command);
      CustomizationGuiRunOnce guiRunOnce = new CustomizationGuiRunOnce();
      guiRunOnce.getCommandList().add(command);
      sprep.setGuiRunOnce(guiRunOnce);
    }
  }

  protected CustomizationGuiUnattended createCustomizationGuiUnattended()
      throws APPlatformException {
    CustomizationGuiUnattended guiUnattended = new CustomizationGuiUnattended();
    guiUnattended.setAutoLogon(false);
    guiUnattended.setAutoLogonCount(0);
    guiUnattended.setTimeZone(DEFAULT_TIMEZONE);
    setAdminPasswordForVm(guiUnattended);
    return guiUnattended;
  }

  protected void setAdminPasswordForVm(CustomizationGuiUnattended guiUnattended)
      throws APPlatformException {
    String adminPwd = paramHandler.getServiceSetting(VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD);

    if ((adminPwd == null || adminPwd.length() == 0)
        && !paramHandler.isServiceSettingTrue(VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN)) {
      logger.error(
          "The VM is not joining a Windows domain. A local administrator password is required but not set.");
      throw new APPlatformException(
          "The VM is not joining a Windows domain. A local administrator password is required but not set.");
    } else if (adminPwd != null && adminPwd.length() > 0) {
      CustomizationPassword password = new CustomizationPassword();
      password.setValue(adminPwd);
      password.setPlainText(true);
      guiUnattended.setPassword(password);
      logger.debug("Set Windows local administrator pwd: " + adminPwd);
    }
  }

  protected void addVmToWindowsWorkgroup(CustomizationSysprep sprep) {
    CustomizationIdentification identification = new CustomizationIdentification();
    String workgroup =
        paramHandler.getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_WORKGROUP);
    identification.setJoinWorkgroup(workgroup);
    sprep.setIdentification(identification);
    logger.debug("Create workgroup " + workgroup);
  }

  protected void addVmToWindowsDomain(CustomizationSysprep sprep) {
    CustomizationIdentification identification = new CustomizationIdentification();
    String domainName = paramHandler.getServiceSettingValidated(VMPropertyHandler.TS_DOMAIN_NAME);
    String domainAdmin =
        paramHandler.getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN);
    String domainAdminPwd =
        paramHandler.getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD);

    logger.debug(
        "Join Domain " + domainName + " admin: " + domainAdmin + " pwd: " + domainAdminPwd);

    identification.setJoinDomain(domainName);
    identification.setDomainAdmin(domainAdmin);
    CustomizationPassword password = new CustomizationPassword();
    password.setValue(domainAdminPwd);
    password.setPlainText(true);
    identification.setDomainAdminPassword(password);
    sprep.setIdentification(identification);
  }

  protected void configureLinuxSpec(CustomizationSpec cspec) {
    CustomizationGlobalIPSettings gIP = createCustomizationGloablIPSettings();
    cspec.setGlobalIPSettings(gIP);
    String[] dnsserver = paramHandler.getDNSServer(1).split(",");
    for (String server : dnsserver) {
      logger.debug("Linux -> CustomizationGlobalIPSettings -> DNS server: " + server);
      gIP.getDnsServerList().add(server.trim());
    }

    String[] dnssuffix = paramHandler.getDNSSuffix(1).split(",");
    for (String suffix : dnssuffix) {
      logger.debug("Linux -> CustomizationGlobalIPSettings -> DNS suffix: " + suffix);
      gIP.getDnsSuffixList().add(suffix.trim());
    }

    CustomizationLinuxPrep sprep = createCustomizationLinuxPrep();

    String domain = paramHandler.getServiceSetting(VMPropertyHandler.TS_DOMAIN_NAME);
    logger.debug("Linux domain name: " + domain);
    if (domain != null) {
      sprep.setDomain(domain);
    }

    sprep.setHostName(new CustomizationVirtualMachineName());
    sprep.setTimeZone("363");
    sprep.setHwClockUTC(Boolean.TRUE);
    cspec.setIdentity(sprep);
    cspec.setOptions(new CustomizationLinuxOptions());
  }

  protected CustomizationLinuxPrep createCustomizationLinuxPrep() {
    CustomizationLinuxPrep sprep = new CustomizationLinuxPrep();
    return sprep;
  }

  protected CustomizationGlobalIPSettings createCustomizationGloablIPSettings() {
    CustomizationGlobalIPSettings gIP = new CustomizationGlobalIPSettings();
    return gIP;
  }

  protected CustomizationSpec createCustomizationSpec() {
    return new CustomizationSpec();
  }

  protected void validateOsIsLinuxOrWindows(
      VirtualMachineConfigInfo configSpec, boolean isLinux, boolean isWindows)
      throws APPlatformException {
    if (!isLinux && !isWindows) {
      logger.error(
          "GuestId cannot be interpreted. guestid: "
              + configSpec.getGuestId()
              + " OS: "
              + configSpec.getGuestFullName());
      throw new APPlatformException(
          "Unsupported operating system " + configSpec.getGuestFullName());
    }
  }

  protected String getGuestId(VirtualMachineConfigInfo configSpec) throws APPlatformException {
    String guestid = configSpec.getGuestId();
    if (guestid == null) {
      throw new APPlatformException("Operatingsystem not defined in Guest-Id.");
    }
    return guestid;
  }

  private boolean isWindows(String guestid) {
    boolean isWindows = guestid.startsWith("win");
    return isWindows;
  }

  protected boolean isLinux(String guestid) {
    boolean isLinux =
        guestid.startsWith("cent")
            || guestid.startsWith("debian")
            || guestid.startsWith("freebsd")
            || guestid.startsWith("oracle")
            || guestid.startsWith("other24xLinux")
            || guestid.startsWith("other26xLinux")
            || guestid.startsWith("otherLinux")
            || guestid.startsWith("coreos")
            || guestid.startsWith("redhat")
            || guestid.startsWith("rhel")
            || guestid.startsWith("sles")
            || guestid.startsWith("suse")
            || guestid.startsWith("ubuntu");
    return isLinux;
  }

  private void configureManually(boolean isWindows, int i, CustomizationIPSettings ipSettings) {
    setFixIp(i, ipSettings);
    String[] gateways = paramHandler.getGateway(i).split(",");
    for (String gw : gateways) {
      logger.debug("NIC" + i + " Gateway:" + gw);
      ipSettings.getGateway().add(gw.trim());
    }

    if (isWindows) {
      String[] dnsserver = paramHandler.getDNSServer(i).split(",");
      for (String server : dnsserver) {
        logger.debug("NIC" + i + " DNS server:" + server);
        ipSettings.getDnsServerList().add(server.trim());
      }
    }
    logger.debug("NIC" + i + " Subnetmask:" + paramHandler.getSubnetMask(i));
    ipSettings.setSubnetMask(paramHandler.getSubnetMask(i).trim());
  }

  private void configureByPortgroupIPPool(
      boolean isWindows, int i, CustomizationIPSettings ipSettings) {
    String gateway = "0.0.0.0";
    String dnsserver = "0.0.0.0";
    String subnetMask = "0.0.0.0";
    ipSettings.getGateway().add(gateway);
    if (isWindows) {
      ipSettings.getDnsServerList().add(dnsserver);
    }
    ipSettings.setSubnetMask(subnetMask);
    setFixIp(i, ipSettings);
  }

  private void setFixIp(int i, CustomizationIPSettings ipSettings) {
    logger.debug("NIC" + i + " IP:" + paramHandler.getIpAddress(i));
    CustomizationFixedIp newip = new CustomizationFixedIp();
    newip.setIpAddress(paramHandler.getIpAddress(i));
    ipSettings.setIp(newip);
  }
}
