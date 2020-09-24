/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: Sep 8, 2020
 *
 * <p>*****************************************************************************
 */
package org.oscm.app.approval.data;

/**
 * Enumeration of the possible internal statuses of provisioning operations that may be set by the
 * controller and the status dispatcher.
 */
public enum State {

  /** The creation of a new application instance was started. */
  CREATION_REQUESTED,

  /** The application instance is currently being created. */
  CREATING,

  /** A modification of an application instance was started. */
  MODIFICATION_REQUESTED,

  /** The application instance is currently being modified. */
  UPDATING,

  /** The deletion of an application instance was started. */
  DELETION_REQUESTED,

  /** The application instance is currently being deleted. */
  DELETING,

  /** The application instance has been deleted. */
  DELETED,

  /** The creation or modification of an application instance failed. */
  FAILED,

  /** The creation or modification of an application instance has been completed successfully. */
  FINISHED
}
