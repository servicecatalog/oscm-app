/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 16.11.2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.v2_0.data.ControllerSettings;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({ApprovalControllerAccess.class})
public class ApprovalControllerAccessTest {

  private ApprovalControllerAccess approvalControllerAccess;
  private ControllerSettings settings;

  @Before
  public void setup() {
    approvalControllerAccess = PowerMockito.spy(new ApprovalControllerAccess());
    settings = mock(ControllerSettings.class);
  }

  @Test
  public void testGetControllerId() {

    String result = approvalControllerAccess.getControllerId();

    assertEquals(ApprovalController.ID, result);
  }

  @Test
  public void testGetMessage() {

    String result = approvalControllerAccess.getMessage("en", "key", "Message", "about controller access");

    assertEquals("!key!", result);
  }

  @Test
  public void testGetControllerParameterKeys() {

    List<String> result = approvalControllerAccess.getControllerParameterKeys();

    assertEquals(0, result.size());
  }

  @Test
  public void testStoreSettings() {

    approvalControllerAccess.storeSettings(settings);

    assertEquals(settings, approvalControllerAccess.getSettings());
  }
}
