package org.oscm.app.vmware.usage;

import java.net.MalformedURLException;
import java.util.List;

import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.v2_0.exceptions.ConfigurationException;
import org.oscm.app.vmware.business.VM;
import org.oscm.app.vmware.business.VMPropertyHandler;
import org.oscm.app.vmware.remote.vmware.VMClientPool;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.oscm.types.exceptions.ObjectNotFoundException;
import org.oscm.types.exceptions.OrganizationAuthoritiesException;
import org.oscm.types.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NetworkSummary;
import com.vmware.vim25.PerfInterval;

public class VMUsageConverter {

    static final String EVENT_DISK = "EVENT_DISK_GIGABYTE_HOURS";
    static final String EVENT_CPU = "EVENT_CPU_HOURS";
    static final String EVENT_RAM = "EVENT_RAM_MEGABYTE_HOURS";
    static final String EVENT_TOTAL = "EVENT_TOTAL_HOURS";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(VMUsageConverter.class);

    VMPropertyHandler ph;

    public VMUsageConverter() {
    }

    public VMUsageConverter(VMPropertyHandler ph) throws MalformedURLException {
        this.ph = ph;
    }

    public void registerUsageEvents(String startTime, String endTime)
            throws MalformedURLException,
            ObjectNotFoundException, OrganizationAuthoritiesException,
            ValidationException, APPlatformException {

        VM vm = getVM(ph.getInstanceName());
    }

    private VM getVM(String instanceName) {
        String vcenter = ph
                .getServiceSetting(VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
        VM vm = null;
        try {
            VMwareClient vmw = VMClientPool.getInstance().getPool()
                    .borrowObject(vcenter);
            ManagedObjectReference vmInstance = vmw.getServiceUtil().getDecendentMoRef(null, "VirtualMachine", instanceName);
            ManagedObjectReference performanceManager =
                    (ManagedObjectReference)
                        vmw.getServiceUtil().getDynamicProperty(vmInstance, "performanceManager");
            
            
            PerfInterval[] perfIntervals =
                    (PerfInterval[])
                        vmw.getServiceUtil().getDynamicProperty(performanceManager, "historicalInterval");
            
            for (PerfInterval perfInterval : perfIntervals) {
                System.out.println("key = " + perfInterval.getKey());
                System.out.println("length = " + perfInterval.getLength());
                System.out.println("samplingPeriod = " + perfInterval.getSamplingPeriod());
                System.out.println("level = " + perfInterval.getLevel());
                System.out.println("name = " + perfInterval.getName());
                System.out.println();
        }
            
            
            vm = new VM(vmw, ph.getInstanceName());
        } catch (Exception e) {
            LOGGER.error("Can´t gather usage data for instance "
                    + ph.getInstanceId() + "\n" + e.getMessage());
        }
        return vm;
    }
}
