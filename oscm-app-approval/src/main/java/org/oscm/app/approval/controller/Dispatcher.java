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
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Dispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(Dispatcher.class);

  private String instanceId;
  private PropertyHandler paramHandler;
  private APPlatformService platformService;

  public Dispatcher(
      APPlatformService platformService, String instanceId, PropertyHandler paramHandler) {
    this.platformService = platformService;
    this.instanceId = instanceId;
    this.paramHandler = paramHandler;
  }

  public InstanceStatus dispatch() {

    State currentState = paramHandler.getState();

    State newState;

    switch (currentState) {
      case CREATION_REQUESTED:
        newState = State.CREATING;
        break;

      case CREATING:

      case UPDATING:
        newState = State.FINISHED;
        break;

      case MODIFICATION_REQUESTED:
        newState = State.UPDATING;
        break;

      case DELETION_REQUESTED:
        newState = State.DELETING;
        break;

      case DELETING:
        newState = State.DELETED;
        break;

      default:
        newState = State.FAILED;
        break;
    }

    paramHandler.setState(newState);

    InstanceStatus result = new InstanceStatus();
    result.setIsReady(
        State.FINISHED.equals(paramHandler.getState())
            || State.DELETED.equals(paramHandler.getState()));
    result.setChangedParameters(paramHandler.getSettings().getParameters());

    // When provisioning is done, provide access information that can be
    // shown to the subscriber.
    if (result.isReady()) {
        
      result.setAccessInfo("Access information for instance " + instanceId);
    }

    /*if (Status.WAITING_FOR_ACTIVATION.equals(paramHandler.getState())) {
      result.setRunWithTimer(false);
    }*/

    return result;
  }
}
