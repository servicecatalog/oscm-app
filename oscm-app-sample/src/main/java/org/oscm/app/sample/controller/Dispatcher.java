/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Sample controller implementation for the 
 *  Asynchronous Provisioning Platform (APP)
 *       
 *  Creation Date: 2012-09-06                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.sample.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import org.oscm.app.sample.i18n.Messages;
import org.oscm.app.v2_0.data.InstanceStatus;
import org.oscm.app.v2_0.data.LocalizedText;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.intf.APPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher for triggering the next step in a provisioning operation depending
 * on the current internal status.
 * <p>
 * The controller methods for creating, updating, and deleting instances set
 * their own, internal status. This status is evaluated and handled by the
 * dispatcher, which is invoked at regular intervals by APP through the
 * <code>getInstanceStatus</code> method of the <code>SampleController</code>
 * class. The dispatcher sets the next internal status and returns the
 * corresponding overall instance status to APP through the controller.
 * <p>
 * In this sample implementation, the dispatcher simply moves from one step to
 * the next and sends emails at specific status transitions. The recipient and
 * contents of the emails are set as service parameters in the technical service
 * definition of the sample.
 * 
 */
public class Dispatcher {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Dispatcher.class);

    // The ID of the application instance
    private String instanceId;

    // A property handler of the sample implementation
    private PropertyHandler paramHandler;

    // An APPlatformService instance which provides for email communication
    private APPlatformService platformService;

    private static final String EVENT_KEY_NOTIFY = "notify";
    private static final String EVENT_KEY_RESUME = "_resume";
    private static final String EVENT_VALUE_YES = "yes";

    /**
     * Constructs a new dispatcher.
     * 
     * @param platformService
     *            an <code>APPlatformService</code> instance which provides
     *            helper methods for accessing common APP utilities, for
     *            example, send emails
     * @param instanceId
     *            the ID of the application instance in question
     * @param paramHandler
     *            a property handler for reading and writing service parameters
     *            and controller configuration settings
     */
    public Dispatcher(APPlatformService platformService, String instanceId,
            PropertyHandler paramHandler) {
        this.platformService = platformService;
        this.instanceId = instanceId;
        this.paramHandler = paramHandler;

    }

    /**
     * Triggers the next step of a provisioning operation depending on the
     * operation's current internal status, and returns the overall instance
     * status. The internal status is set by the service controller or the
     * dispatcher itself. At specific status transitions, emails are sent to the
     * recipient specified as a service parameter in the technical service
     * definition.
     * <p>
     * In real-life scenarios, the dispatcher might also check the current
     * status or the result of operations triggered from the outside.
     * 
     * @return an <code>InstanceStatus</code> instance with the overall status
     *         of the application instance
     * @throws APPlatformException
     */
    public InstanceStatus dispatch() throws APPlatformException {
        // Get and trace current internal status of the operation
        Status currentState = paramHandler.getState();
        LOGGER.debug("  currentState=" + currentState.toString());

        Status newStatus = null;

        // Dispatch next step depending on current internal status
        switch (currentState) {
        case CREATION_REQUESTED:
            newStatus = Status.CREATION_STEP1;
            break;

        case MODIFICATION_REQUESTED:
            newStatus = Status.UPDATING;
            break;

        case DELETION_REQUESTED:
            newStatus = Status.DELETING;
            break;

        case ACTIVATION_REQUESTED:
            newStatus = Status.FINISHED;
            break;

        case DEACTIVATION_REQUESTED:
            newStatus = Status.FINISHED;
            break;

        case CREATION_STEP1:
            platformService.lockServiceInstance("ess.sample", instanceId,
                    paramHandler.getTPAuthentication());
            
            if (null == paramHandler.getAppBaseUrl()
                    || paramHandler.getAppBaseUrl().isEmpty()) {
                newStatus = Status.CREATION_STEP2;
            } else
                newStatus = Status.MANUAL_CREATION;
            break;

        case CREATION_STEP2:
            platformService.unlockServiceInstance("ess.sample", instanceId,
                    paramHandler.getTPAuthentication());
            newStatus = Status.FINISHED;
            sendMail(instanceId,currentState);
            break;

        case MANUAL_CREATION:
            platformService.unlockServiceInstance("ess.sample", instanceId,
                    paramHandler.getTPAuthentication());
            sendMail(instanceId, currentState);
            newStatus = Status.WAITING_FOR_ACTIVATION;
            break;
            
        case WAITING_FOR_ACTIVATION:
            //do nothing, just wait until the status changes
            break;
        
        case FINSIHING_MANUAL_PROVISIONING:
            newStatus = Status.FINISHED;
            break;
            
        case UPDATING:
            newStatus = Status.FINISHED;
            sendMail(instanceId, currentState);
            break;

        case DELETING:
            newStatus = Status.DESTROYED;
            break;

        default:
        }

        if (newStatus != null) {
            // Set the next internal status for the provisioning operation.
            // The status is stored as a controller configuration setting.
            LOGGER.debug("  newState=" + newStatus.toString());
            paramHandler.setState(newStatus);
        }

        InstanceStatus result = new InstanceStatus();

        // Set the overall status of the application instance.
        // The instance is ready if the internal status of the
        // provisioning operation is FINISHED or DESTROYED. If this
        // is the case, APP stops polling for the instance status.
        result.setIsReady(paramHandler.getState() == Status.FINISHED
                || paramHandler.getState() == Status.DESTROYED);

        // Update the description of the instance status.
        // This description is displayed to users for a pending
        // subscription.
        List<LocalizedText> messages = Messages.getAll("status_" + paramHandler.getState());
        result.setDescription(messages);

        // Return the current parameters and settings to APP.
        // They are stored in the APP database.
        result.setChangedParameters(paramHandler.getSettings().getParameters());

        // When provisioning is done, provide access information that can be
        // shown to the subscriber.
        if (result.isReady()) {
            result.setAccessInfo("Access information for instance " + instanceId);
        }
        
        if(Status.WAITING_FOR_ACTIVATION.equals(paramHandler.getState())){
            result.setRunWithTimer(false);
        }
        
        return result;
    }
    
    private String getSampleText(Status currentState) {
        return Messages.get(Messages.DEFAULT_LOCALE, "mail.text",
                new Object[] { instanceId, paramHandler.getMessage(),
                        currentState.toString() });
    }

    protected String getManualProvisioningText(String eventLink) {
        return Messages.get(Messages.DEFAULT_LOCALE,
                "mail.text.manual.provisioning",
                new Object[] { paramHandler.getMessage(), eventLink});
    }

    protected String getEventLink() {
        StringBuffer eventLink = new StringBuffer();
        try {
            eventLink.append(paramHandler.getAppBaseUrl()).append("/")
                    .append(EVENT_KEY_NOTIFY).append("?").append("sid=")
                    .append(URLEncoder.encode(instanceId, "UTF-8")).append('&')
                    .append("controllerid=")
                    .append(URLEncoder.encode(
                            paramHandler.getSettings().getParameters()
                                    .get("APP_CONTROLLER_ID").getValue(),
                            "UTF-8"))
                    .append('&').append(EVENT_KEY_RESUME).append('=')
                    .append(EVENT_VALUE_YES);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Failed to create event link", e);
        }
        return eventLink.toString();
    }

    /**
     * Sends an email with the contents and to the recipient specified in the
     * technical service definition and includes all parameters and attributes.
     * 
     * @param instanceId
     *            the ID of the application instance in question
     * @param currentState
     *            the current internal status of the provisioning operation
     * @throws APPlatformException
     *             - if an error occurred on mail delivery
     */
    protected void sendMail(String instanceId, Status currentState)
            throws APPlatformException {
        // Create mail subject and contents
        String subject = Messages.get(Messages.DEFAULT_LOCALE, "mail.subject",
                new Object[] { instanceId });
        
        String text;
        Email mail = Email.get(paramHandler.getSettings());
        if(Status.MANUAL_CREATION == paramHandler.getState()) {
            text = getManualProvisioningText(mail.createConfirmationLink(instanceId));
        } else {
            text = getSampleText(currentState);
        }

        // Send to configured recipients
        mail.send(Collections.singletonList(paramHandler.getEMail()), subject,
                text);
    }

}
