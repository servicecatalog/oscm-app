package org.oscm.app.vmware.usage;


import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.VMPropertyHandler;
import com.vmware.vim25.RuntimeFaultFaultMsg;

public class VMUsageCalculator {

    VMPropertyHandler ph;
    VMMetricCollector collector;
    
    
    public VMUsageCalculator(VMPropertyHandler ph) {
        this.ph = ph;
        collector = new VMMetricCollector(ph);
        collector.createMetricforInstance();
    }
    
    
    public long calculateMemUsage() throws NumberFormatException, APPlatformException, RuntimeFaultFaultMsg {
        long memUsagePercent = Integer.parseInt(collector.getDiskUsageTotal()) / 100;
        long memUsageMB = (int) (ph.getConfigMemoryMB() * memUsagePercent / 100);
        long memUsageGB = memUsageMB / 1024;
        return memUsageGB;
        
    }
    
    public long calculateDiskUsageGB() throws NumberFormatException, APPlatformException, RuntimeFaultFaultMsg {
        long diskUsageKB = Integer.parseInt(collector.getDiskUsageTotal());
        long diskUsageMB = diskUsageKB / 1024;
        long diskUsageGB = diskUsageMB / 1024;
        return diskUsageGB;
    }
    
    public long calculateCpuUsage() throws NumberFormatException, APPlatformException, RuntimeFaultFaultMsg {
        return Integer.parseInt(collector.getCpuUsage());
    }
    
}
