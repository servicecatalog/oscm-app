package org.oscm.app.vmware.usage;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VMUsageCalculator {

    VMPropertyHandler ph;
    VMMetricCollector collector;

    
    public VMUsageCalculator() {
    }

    public VMUsageCalculator(VMPropertyHandler ph) {
        this.ph = ph;
        collector = new VMMetricCollector(ph);
        collector.initialize();
    }
    
    public VMUsageCalculator(VMPropertyHandler ph, VMMetricCollector collector) {
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

}
