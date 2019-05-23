/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2018
 *                                                                              
 *  Creation Date: 17.08.2010                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.business;

import org.oscm.app.business.exceptions.BadResultException;
import org.oscm.app.domain.InstanceParameter;
import org.oscm.app.domain.ServiceInstance;
import org.oscm.provisioning.intf.ProvisioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Implementation for the product provisioning service factory.
 * 
 * @author Mike J&auml;ger
 * 
 */
@Stateless
public class ProductProvisioningServiceFactoryBean {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ProductProvisioningServiceFactoryBean.class);

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public ProvisioningService getInstance(ServiceInstance instance)
            throws BadResultException {

        ProvisioningService result = null;
        try {
            InstanceParameter publicIpParam = instance
                    .getParameterForKey(InstanceParameter.PUBLIC_IP);
            if (publicIpParam == null) {
                BadResultException bre = new BadResultException(String.format(
                        "Parameter for key '%s' not found",
                        InstanceParameter.PUBLIC_IP));
                LOGGER.warn(bre.getMessage(), bre);
                throw bre;
            }
            String ip = publicIpParam.getDecryptedValue();
            URL wsdlUrl = getServiceWsdlUrl(instance, ip);

            InstanceParameter serviceUserParam = instance
                    .getParameterForKey(InstanceParameter.SERVICE_USER);
            InstanceParameter serviceUserPwdParam = instance
                    .getParameterForKey(InstanceParameter.SERVICE_USER_PWD);

            //TODO: add additional check for user
            String username = (serviceUserParam == null) ? null
                    : serviceUserParam.getDecryptedValue();
            String password = (serviceUserPwdParam == null) ? null
                    : serviceUserPwdParam.getDecryptedValue();

            try {
                // TODO: think of common WS client (BesDAO is also using webservice connection)
                String targetNamespace =
                    ProvisioningService.class.getAnnotation(WebService.class).targetNamespace();
                QName serviceQName =
                        new QName(targetNamespace, ProvisioningService.class.getSimpleName());

                Service service = Service.create(wsdlUrl, serviceQName);
                result = service.getPort(ProvisioningService.class);

            } catch (WebServiceException e) {
                result = null;
                BadResultException bre = new BadResultException(
                        String.format(
                                "Failed to retrieve service related provisioning service for instance '%s'",
                                String.valueOf(instance.getTkey())), e);
                LOGGER.warn(bre.getMessage(), bre);
                throw bre;
            }

        } catch (Exception e) {
            BadResultException ex = new BadResultException(
                    "Communication with service failed", e);
            LOGGER.warn(ex.getMessage(), ex);
            throw ex;
        }
        return result;
    }

    private URL getServiceWsdlUrl(ServiceInstance instance, String ip)
            throws BadResultException, MalformedURLException {
        InstanceParameter provServWSDLParam = instance
                .getParameterForKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_WSDL);
        if (provServWSDLParam == null) {
            BadResultException bre = new BadResultException(String.format(
                    "Parameter for key '%s' not found",
                    InstanceParameter.SERVICE_RELATIVE_PROVSERV_WSDL));
            LOGGER.warn(bre.getMessage(), bre);
            throw bre;
        }
        InstanceParameter provServProtocolParam = instance
                .getParameterForKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_PROTOCOL);
        if (provServProtocolParam == null) {
            BadResultException bre = new BadResultException(String.format(
                    "Parameter for key '%s' not found",
                    InstanceParameter.SERVICE_RELATIVE_PROVSERV_PROTOCOL));
            LOGGER.warn(bre.getMessage(), bre);
            throw bre;
        }
        InstanceParameter provServPortParam = instance
                .getParameterForKey(InstanceParameter.SERVICE_RELATIVE_PROVSERV_PORT);
        if (provServPortParam == null) {
            BadResultException bre = new BadResultException(String.format(
                    "Parameter for key '%s' not found",
                    InstanceParameter.SERVICE_RELATIVE_PROVSERV_PORT));
            LOGGER.warn(bre.getMessage(), bre);
            throw bre;
        }

        String relativePath = provServWSDLParam.getDecryptedValue();
        String separator = "/";
        if (relativePath.startsWith(separator)) {
            separator = "";
        }
        URL wsdlUrl = new URL(provServProtocolParam.getDecryptedValue() + "://"
                + ip + ":" + provServPortParam.getDecryptedValue() + separator
                + provServWSDLParam.getDecryptedValue());
        return wsdlUrl;
    }
}
