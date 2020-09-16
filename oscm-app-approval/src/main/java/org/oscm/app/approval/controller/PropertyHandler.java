/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: Sep 8, 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.controller;

import org.oscm.app.approval.data.State;
import org.oscm.app.v2_0.data.ProvisioningSettings;
import org.oscm.app.v2_0.data.Setting;

import java.util.Map;

/**
 * Helper class to handle service parameters and controller configuration settings. The
 * implementation shows how the settings can be managed in a centralized way.
 *
 * <p>The underlying <code>ProvisioningSettings</code> object of APP provides all the specified
 * service parameters and controller configuration settings (key/value pairs). The settings are
 * stored in the APP database and therefore available even after restarting the application server.
 */
public class PropertyHandler {

  private ProvisioningSettings settings;

  /**
   * The internal status of a provisioning operation as set by the controller or the status
   * dispatcher.
   */
  public static final String STATE = "STATE";

  /**
   * Default constructor.
   *
   * @param settings a <code>ProvisioningSettings</code> object specifying the service parameters
   *     and configuration settings
   */
  public PropertyHandler(ProvisioningSettings settings) {
    this.settings = settings;
  }

  /**
   * Returns the internal state of the current provisioning operation as set by the controller or
   * the dispatcher.
   *
   * @return the current status
   */
  public State getState() {
    String status = getValue(STATE, settings.getParameters());
    return (status != null) ? State.valueOf(status) : State.FAILED;
  }

  /**
   * Changes the internal state for the current provisioning operation.
   *
   * @param newState the new state to set
   */
  public void setState(State newState) {
    setValue(STATE, newState.toString(), settings.getParameters());
  }

  /**
   * Returns the current service parameters and controller configuration settings.
   *
   * @return a <code>ProvisioningSettings</code> object specifying the parameters and settings
   */
  public ProvisioningSettings getSettings() {
    return settings;
  }

  private String getValue(String key, Map<String, Setting> source) {
    Setting setting = source.get(key);
    return setting != null ? setting.getValue() : null;
  }

  private void setValue(String key, String value, Map<String, Setting> target) {
    target.put(key, new Setting(key, value));
  }
}
