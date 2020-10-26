package org.oscm.app.common.ui;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.oscm.app.v2_0.APPlatformServiceFactory;
import org.oscm.app.v2_0.data.ControllerConfigurationKey;
import org.oscm.app.v2_0.data.PasswordAuthentication;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.oscm.app.v2_0.intf.ControllerAccess;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(APPlatformServiceFactory.class)
public class ConfigurationBeanTest {

  private ConfigurationBean bean;

  @Mock private APPlatformService platformService;
  @Mock private ControllerAccess controllerAccess;
  @Mock private FacesContext facesCtx;

  @Mock private ExternalContext externalCtx;
  @Mock private HttpSession httpSession;
  @Mock private UIViewRoot viewRoot;

  @Before
  public void setupClass() {
    PowerMockito.mockStatic(APPlatformServiceFactory.class);
    when(APPlatformServiceFactory.getInstance()).thenReturn(platformService);

    bean = spy(new ConfigurationBean());
    bean.setControllerAccess(controllerAccess);

    doReturn(facesCtx).when(bean).getContext();
    when(facesCtx.getExternalContext()).thenReturn(externalCtx);
    when(externalCtx.getSession(anyBoolean())).thenReturn(httpSession);
    when(facesCtx.getViewRoot()).thenReturn(viewRoot);
    when(viewRoot.getLocale()).thenReturn(Locale.getDefault());
  }

  @Test
  public void testGetInitialize() {
    // when
    bean.getInitialize();

    // then
    assertFalse(bean.isSaved());
    assertFalse(bean.isDirty());
  }

  @Test
  public void testGetItems() throws Exception {
    // given
    HashMap<String, Setting> settings = new HashMap<>();
    settings.put(
        ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(),
        new Setting(ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(), "orgName"));
    settings.put(
        ControllerConfigurationKey.BSS_USER_ID.name(),
        new Setting(ControllerConfigurationKey.BSS_USER_ID.name(), "userId"));
    settings.put(
        ControllerConfigurationKey.BSS_USER_KEY.name(),
        new Setting(ControllerConfigurationKey.BSS_USER_KEY.name(), "userKey"));

    when(platformService.getControllerSettings(anyString(), any(PasswordAuthentication.class)))
        .thenReturn(settings);
    when(controllerAccess.getControllerParameterKeys())
        .thenReturn(
            Arrays.asList(
                ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(),
                ControllerConfigurationKey.BSS_USER_ID.name(),
                ControllerConfigurationKey.BSS_USER_KEY.name()));

    // when
    List<ConfigurationItem> items = bean.getItems();

    // then
    assertFalse(bean.isUpdated());
    assertEquals(settings.size(), items.size());
  }

  @Test
  public void testGetAccessItems() throws Exception {

    // given
    HashMap<String, Setting> settings = new HashMap<>();
    settings.put(
        ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(),
        new Setting(ControllerConfigurationKey.BSS_ORGANIZATION_ID.name(), "orgName"));
    settings.put(
        ControllerConfigurationKey.BSS_USER_ID.name(),
        new Setting(ControllerConfigurationKey.BSS_USER_ID.name(), "userId"));
    settings.put(
        ControllerConfigurationKey.BSS_USER_KEY.name(),
        new Setting(ControllerConfigurationKey.BSS_USER_KEY.name(), "userKey"));
    settings.put(
        ControllerConfigurationKey.BSS_USER_PWD.name(),
        new Setting(ControllerConfigurationKey.BSS_USER_PWD.name(), "userPwd"));

    when(platformService.getControllerSettings(anyString(), any(PasswordAuthentication.class)))
        .thenReturn(settings);

    // when
    List<ConfigurationItem> items = bean.getAccessItems();

    // then
    assertFalse(bean.isUpdated());
    assertEquals(settings.size(), items.size());
  }

  @Test
  public void testSave() throws Exception {
    // given
    doReturn(true).when(bean).isTokenValid();

    // when
    bean.save();

    // then
    verify(platformService, times(1))
        .storeControllerSettings(anyString(), any(), any(PasswordAuthentication.class));
    assertTrue(bean.isSaved());
  }

  @Test
  public void testGetConfigurationTitle() {
    // given
    String msgTitle = "!" + ConfigurationBean.MSG_CONFIG_TITLE + "!";
    when(controllerAccess.getMessage(anyString(), anyString())).thenReturn(msgTitle);
    when(controllerAccess.getControllerId()).thenReturn("ess.sample");

    // when
    String title = bean.getConfigurationTitle();

    // then
    assertEquals("Controller Configuration (ess.sample)", title);
  }

  @Test
  public void testGetSettingsTitle() {
    // given
    String msgTitle = "!" + ConfigurationBean.MSG_SETTINGS_TITLE + "!";
    when(controllerAccess.getMessage(anyString(), anyString())).thenReturn(msgTitle);
    when(controllerAccess.getControllerId()).thenReturn("ess.sample");

    // when
    String title = bean.getSettingsTitle();

    // then
    assertEquals("Controller settings (ess.sample)", title);
  }

  @Test
  public void testGetLoggedInUserId() {
    // given
    String userId = "userId";
    when(httpSession.getAttribute("loggedInUserId")).thenReturn(userId);

    // when
    String loggedInUserId = bean.getLoggedInUserId();

    // then
    assertEquals(userId, loggedInUserId);
  }

  @Test
  public void testApplyCurrentUser() {
    // given
    bean.getItems();

    // when
    bean.applyCurrentUser();

    // then
    verify(httpSession, times(5)).getAttribute(anyString());
  }
}
