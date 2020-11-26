/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2020
 *
 *  Creation Date: Mar 26, 2020
 *
 *******************************************************************************/
package org.oscm.app.sample.controller;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SampleControllerAccessTest {

  private SampleControllerAccess ctrlAccess = new SampleControllerAccess();

  @Test
  public void testGetControllerId() {
    // when
    String controllerId = ctrlAccess.getControllerId();
    // then
    assertEquals(SampleController.ID, controllerId);
  }

  @Test
  public void testGetControllerParameterKeys() {
    // when
    List<String> keys = ctrlAccess.getControllerParameterKeys();
    // then
    assertTrue(keys.contains(PropertyHandler.TECPARAM_USER));
    assertTrue(keys.contains(PropertyHandler.TECPARAM_PWD));
    assertTrue(keys.contains(PropertyHandler.TECPARAM_EMAIL));
    assertTrue(keys.contains(PropertyHandler.TECPARAM_MESSAGETEXT));
  }
}
