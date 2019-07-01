package org.oscm.app.vmware.usage;

import static java.time.LocalDateTime.parse;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.net.MalformedURLException;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.exceptions.ConfigurationException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.intf.EventService;
import org.oscm.types.exceptions.DuplicateEventException;
import org.oscm.types.exceptions.ObjectNotFoundException;
import org.oscm.types.exceptions.OrganizationAuthoritiesException;
import org.oscm.types.exceptions.ValidationException;
import org.oscm.vo.VOGatheredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VMUsageConverter {

    static final String EVENT_DISK = "EVENT_DISK_GIGABYTE_HOURS";
    static final String EVENT_CPU = "EVENT_CPU_HOURS";
    static final String EVENT_RAM = "EVENT_RAM_MEGABYTE_HOURS";
    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(VMUsageConverter.class);

    VMPropertyHandler ph;
    
    public VMUsageConverter() {
        
    }

    public VMUsageConverter(VMPropertyHandler ph) throws MalformedURLException {
        this.ph = ph;
    }
    

    public void registerUsageEvents(String startTime, String endTime)
            throws NumberFormatException, APPlatformException,
            RuntimeFaultFaultMsg, ObjectNotFoundException, MalformedURLException, OrganizationAuthoritiesException, ValidationException {

        VMUsageCalculator usage = new VMUsageCalculator(ph);

            submit(EVENT_RAM, usage.calculateMemUsageMB(), endTime);
            submit(EVENT_CPU, usage.calculateCpuUsageMhz(), endTime);
            submit(EVENT_DISK, usage.calculateDiskUsageGB(), endTime);

    } 

    void submit(String eventId, long multiplier, String occurence)
            throws ConfigurationException, MalformedURLException,
            ObjectNotFoundException, OrganizationAuthoritiesException,
            ValidationException {

        if (multiplier <= 0) {
            return;
        }

        VOGatheredEvent event = new VOGatheredEvent();
        event.setActor(ph.getSettings().getRequestingUser().getUserId());
        event.setEventId(eventId);
        event.setMultiplier(multiplier);
        event.setOccurrenceTime(parse(occurence, ISO_LOCAL_DATE_TIME)
                .toInstant(UTC).toEpochMilli());
        event.setUniqueId(eventId + "_" + occurence);

        try {
            EventService svc = ph.getWebService(EventService.class);
            svc.recordEventForInstance(ph.getTechnicalServiceId(),
                    ph.getInstanceId(), event);
        } catch (DuplicateEventException e) {
            LOGGER.debug("Event already inserted");
        }
    }
}
