package org.oscm.app.approval.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.data.InstanceDescription;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ApprovalController.class})
public class ApprovalControllerTest {

  private ApprovalController approvalController;
  private APPlatformService platformService;
  private ProvisioningSettings settings;
  private PropertyHandler propertyHandler;
  private InstanceDescription instanceDescription;
  private ApprovalInstanceAccess approvalInstanceAccess;


  @Before
  public void setup() {
    approvalController = PowerMockito.spy(new ApprovalController());
    platformService = mock(APPlatformService.class);
    settings = mock(ProvisioningSettings.class);
    propertyHandler = mock(PropertyHandler.class);
    instanceDescription = mock(InstanceDescription.class);
    approvalInstanceAccess = mock(ApprovalInstanceAccess.class);

  }

  @Test
  public void testCreateInstance() throws Exception {

    HashMap<String, Setting> parameters = new HashMap<>();
    Setting setting = new Setting("custom", "dark");
    parameters.put("set", setting);

    PowerMockito.whenNew(PropertyHandler.class).withAnyArguments().thenReturn(propertyHandler);
    PowerMockito.doNothing().when(approvalController, "checkIfAlreadyExisting", anyString());
    when(settings.getParameters()).thenReturn(parameters);

    InstanceDescription result = approvalController.createInstance(settings);

    assertEquals(parameters, result.getChangedParameters());
  }

  @Test(expected = APPlatformException.class)
  public void testCheckIfAlreadyExisting() throws Exception {

    Collection<String> data = new ArrayList<String>() {{ add("Already subscribed"); }};
    PowerMockito.whenNew(ApprovalInstanceAccess.class).withNoArguments().thenReturn(approvalInstanceAccess);
    when(approvalInstanceAccess.getInstancesForOrganization(anyString())).thenReturn(data);

    Whitebox.invokeMethod(approvalController, "checkIfAlreadyExisting", "organizationId");
  }

  @Test
  public void testModifyInstance() throws Exception {

    Collection<String> data = new ArrayList<String>() {{ add("Already subscribed"); }};
    PowerMockito.whenNew(ApprovalInstanceAccess.class).withNoArguments().thenReturn(approvalInstanceAccess);
    when(approvalInstanceAccess.getInstancesForOrganization(anyString())).thenReturn(data);

    approvalController.modifyInstance("instanceId", settings, settings);
  }
}
