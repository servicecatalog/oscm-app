/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 22 Oct 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.vmware.business;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.CustomizationGlobalIPSettings;
import com.vmware.vim25.CustomizationGuiUnattended;
import com.vmware.vim25.CustomizationLinuxPrep;
import com.vmware.vim25.CustomizationPassword;
import com.vmware.vim25.CustomizationSpec;
import com.vmware.vim25.CustomizationSysprep;
import com.vmware.vim25.CustomizationUserData;
import com.vmware.vim25.CustomizationWinOptions;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualMachineConfigInfo;

/** @author worf */
public class CustomizationSpecTemplateTest {

  @Mock VMPropertyHandler paramHandler;
  @Mock VMwareClient vmw;
  @Mock ManagedObjectReference vmDataCenter;
  @Mock ManagedObjectAccessor moa;
  @Mock VirtualMachineConfigInfo configSpec;
  @Mock CustomizationSpec cspec;

  CustomizationSpecTemplate template;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    template = new CustomizationSpecTemplate(paramHandler);
    template = Mockito.spy(template);
  }

  @Test
  public void getCustomizationSpec_isLinux() throws APPlatformException {

    // given
    doReturn("centosGuestId").when(template).getGuestId(configSpec);
    doNothing()
        .when(template)
        .validateOsIsLinuxOrWindows(
            any(VirtualMachineConfigInfo.class), anyBoolean(), anyBoolean());
    doNothing().when(template).configureLinuxSpec(any());
    doNothing().when(template).setIpsForCustomSpec(anyBoolean(), any());

    // when
    template.getCustomizationSpec(configSpec);
    // then
    verify(template, times(1)).configureLinuxSpec(any());
  }

  @Test
  public void getCustomizationSpec_isWindows() throws APPlatformException {

    // given
    doReturn("windowsGuestId").when(template).getGuestId(configSpec);
    doNothing()
        .when(template)
        .validateOsIsLinuxOrWindows(
            any(VirtualMachineConfigInfo.class), anyBoolean(), anyBoolean());
    doNothing().when(template).configureWindowsSpec(any());
    doNothing().when(template).setIpsForCustomSpec(anyBoolean(), any());

    // when
    template.getCustomizationSpec(configSpec);
    // then
    verify(template, times(1)).configureWindowsSpec(any());
  }

  @Test
  public void setIpsForCustomSpec() {

    // given
    doReturn("3").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS);
    doNothing().when(template).setIpForNetworkCard(anyBoolean(), any(), anyInt());
    // when
    template.setIpsForCustomSpec(true, cspec);
    // then

    verify(template, times(3)).setIpForNetworkCard(anyBoolean(), any(), anyInt());
  }

  @Test
  public void configureWindowsSpec_joinDomain() throws APPlatformException {
    // given
    CustomizationGuiUnattended guiUnattended = mock(CustomizationGuiUnattended.class);
    CustomizationUserData userData = mock(CustomizationUserData.class);

    doReturn(true)
        .when(paramHandler)
        .isServiceSettingTrue(VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN);
    doReturn(guiUnattended).when(template).createCustomizationGuiUnattended();
    doReturn(userData).when(template).createVmUserData();
    doNothing().when(template).setSysprepCommand(any());
    doNothing().when(template).addVmToWindowsDomain(any());
    // when
    template.configureWindowsSpec(cspec);

    // then
    verify(template, times(1)).addVmToWindowsDomain(any());
  }

  @Test
  public void configureWindowsSpec_joinWorkgroup() throws APPlatformException {
    // given
    CustomizationGuiUnattended guiUnattended = mock(CustomizationGuiUnattended.class);
    CustomizationUserData userData = mock(CustomizationUserData.class);
    CustomizationWinOptions options = mock(CustomizationWinOptions.class);

    doReturn(false)
        .when(paramHandler)
        .isServiceSettingTrue(VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN);
    doReturn(guiUnattended).when(template).createCustomizationGuiUnattended();
    doReturn(userData).when(template).createVmUserData();
    doReturn(options).when(template).createCustomizationWinOptions();
    doNothing().when(template).setSysprepCommand(any());
    doNothing().when(template).addVmToWindowsWorkgroup(any());
    // when
    template.configureWindowsSpec(cspec);

    // then
    verify(template, times(1)).addVmToWindowsWorkgroup(any());
    verify(cspec, times(1)).setOptions(options);
  }

  @Test
  public void createVmUserData_withLicense() {
    // given
    doReturn("de").when(paramHandler).getLocale();
    doReturn("user").when(paramHandler).getResponsibleUserAsString("de");

    doReturn("licenseKey")
        .when(paramHandler)
        .getServiceSetting(VMPropertyHandler.TS_WINDOWS_LICENSE_KEY);

    // when
    CustomizationUserData result = template.createVmUserData();

    // then
    assertEquals("user", result.getFullName());
    assertEquals("licenseKey", result.getProductId());
  }

  @Test
  public void createVmUserData_withoutLicense() {
    // given
    doReturn("de").when(paramHandler).getLocale();
    doReturn("user").when(paramHandler).getResponsibleUserAsString("de");

    // when
    CustomizationUserData result = template.createVmUserData();

    // then
    assertEquals("user", result.getFullName());
    assertEquals("", result.getProductId());
  }

  @Test
  public void createCustomizationGuiUnattended() throws APPlatformException {
    // given
    doNothing().when(template).setAdminPasswordForVm(any());

    // when
    CustomizationGuiUnattended result = template.createCustomizationGuiUnattended();

    // then
    assertEquals(110, result.getTimeZone());
    assertEquals(0, result.getAutoLogonCount());
  }

  @Test(expected = APPlatformException.class)
  public void setAdminPasswordForVm_noPasswordSet() throws APPlatformException {

    // given
    CustomizationGuiUnattended guiUnattended = mock(CustomizationGuiUnattended.class);
    doReturn("").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD);

    // when
    template.setAdminPasswordForVm(guiUnattended);
  }

  @Test
  public void setAdminPasswordForVm() throws APPlatformException {

    // given
    CustomizationGuiUnattended guiUnattended = mock(CustomizationGuiUnattended.class);
    doReturn("pwd")
        .when(paramHandler)
        .getServiceSetting(VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD);
    // when
    template.setAdminPasswordForVm(guiUnattended);
    // then
    verify(guiUnattended, times(1)).setPassword(any(CustomizationPassword.class));
  }

  @Test
  public void addVmToWindowsWorkgroup() {

    // given
    CustomizationSysprep prep = new CustomizationSysprep();
    doReturn("group")
        .when(paramHandler)
        .getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_WORKGROUP);

    // when
    template.addVmToWindowsWorkgroup(prep);

    // then
    assertEquals("group", prep.getIdentification().getJoinWorkgroup());
  }

  @Test
  public void addVmToWindowsDomain() {

    // given
    CustomizationSysprep prep = new CustomizationSysprep();
    doReturn("domain")
        .when(paramHandler)
        .getServiceSettingValidated(VMPropertyHandler.TS_DOMAIN_NAME);
    doReturn("admin")
        .when(paramHandler)
        .getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN);
    doReturn("pwd")
        .when(paramHandler)
        .getServiceSettingValidated(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD);

    // when
    template.addVmToWindowsDomain(prep);

    // then
    assertEquals("domain", prep.getIdentification().getJoinDomain());
    assertEquals("admin", prep.getIdentification().getDomainAdmin());
  }

  @Test
  public void configureLinuxSpec() {
    // given
    CustomizationGlobalIPSettings gip = mock(CustomizationGlobalIPSettings.class);
    CustomizationLinuxPrep sprep = mock(CustomizationLinuxPrep.class);

    doReturn("DNS").when(paramHandler).getDNSServer(anyInt());
    doReturn("DNS").when(paramHandler).getDNSSuffix(anyInt());
    doReturn("domain").when(paramHandler).getServiceSetting(VMPropertyHandler.TS_DOMAIN_NAME);
    doReturn(gip).when(template).createCustomizationGloablIPSettings();
    doReturn(sprep).when(template).createCustomizationLinuxPrep();
    doReturn(new ArrayList<String>()).when(gip).getDnsServerList();
    doReturn(new ArrayList<String>()).when(gip).getDnsSuffixList();
    // when
    template.configureLinuxSpec(cspec);

    // then
    verify(cspec, times(1)).setOptions(any());
  }
}
