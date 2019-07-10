/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2018
 *
 * <p>Creation Date: 2019-07-09
 *
 * <p>*****************************************************************************
 */


package org.oscm.app.vmware.usage;

import static java.time.LocalDateTime.parse;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

import java.time.format.DateTimeParseException;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.RuntimeFaultFaultMsg;
/**
 * 
 * @author worf
 * calculates the actual usage of mem, cpu and disc usage for the vm
 */
public class VMUsageCalculator {

    private static final Logger logger = LoggerFactory.getLogger(VMUsageCalculator.class);
    
    protected VMPropertyHandler ph;
    protected VMMetricCollector collector;

    
    public VMUsageCalculator() {
    }

    public VMUsageCalculator(VMPropertyHandler ph){
        this.ph = ph;
        collector = new VMMetricCollector(ph);
        collector.initialize();
    }
    
    public VMUsageCalculator(VMPropertyHandler ph, VMMetricCollector collector) throws APPlatformException {
        this.ph = ph;
        this.collector = collector;
        collector.initialize();
    }



    public long calculateMemUsageMB() throws NumberFormatException,
            APPlatformException, RuntimeFaultFaultMsg {
        long memUsageMB =  (ph.getConfigMemoryMB() * collector.getMemUsagePercent()
                / 100);
        return memUsageMB;
    }

    public long calculateDiskUsageGB() throws NumberFormatException,
            APPlatformException, RuntimeFaultFaultMsg {
        long diskUsageMB = collector.getDiskUsageTotalKB() / 1024;
        long diskUsageGB = diskUsageMB / 1024;
        return diskUsageGB;
    }

    public long calculateCpuUsageMhz() throws NumberFormatException,
            APPlatformException, RuntimeFaultFaultMsg {
        return collector.getCpuUsageMhz();
    }

    public long calculateTimeframe(String startTime, String endTime){
        long hoursBetweenTwoDates;
        try {
        final long start = parse(startTime, ISO_LOCAL_DATE_TIME).toInstant(UTC)
                .toEpochMilli();
        final long end = parse(endTime, ISO_LOCAL_DATE_TIME).toInstant(UTC)
                .toEpochMilli();
        hoursBetweenTwoDates = (end - start)/ (60 * 60 * 1000) ;
        if(hoursBetweenTwoDates < 1) {
            hoursBetweenTwoDates = 1;
        }
        } catch(DateTimeParseException e) {
            logger.error("failed to read start time " + startTime + " or end time " + endTime + ". Use 24 hours as default timeframe"); 
        hoursBetweenTwoDates = 24;    
        }
        return hoursBetweenTwoDates;
    }

}
