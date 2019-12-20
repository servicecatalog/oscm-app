/*******************************************************************************
 *                                                                              
 *  Copyright FUJITSU LIMITED 2019                                           
 *                                                                                                                                 
 *  Creation Date: 20 Dec 2019                                                      
 *                                                                              
 *******************************************************************************/

package org.oscm.app.vmware.business;

import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;

/**
 * @author  worf
 */
public class ScriptExecutionObjectReferences {
    
    VimPortType vimPort;
    
    ManagedObjectAccessor moa;
    
    ManagedObjectReference guestOpManger;
    
    ManagedObjectReference fileManagerRef;
    
    ManagedObjectReference processManagerRef;
    
    ScriptExecutionObjectReferences(VMwareClient vmw) throws Exception {
        
        vimPort = vmw.getConnection().getService();
        ServiceConnection conn = new ServiceConnection(vimPort,
                vmw.getConnection().getServiceContent());
        moa = getAccessor(conn);
        guestOpManger = vmw.getConnection()
                .getServiceContent().getGuestOperationsManager();
        fileManagerRef = (ManagedObjectReference) moa
                .getDynamicProperty(guestOpManger, "fileManager");
        processManagerRef = (ManagedObjectReference) moa
                .getDynamicProperty(guestOpManger, "processManager");
    }
    
    protected ManagedObjectAccessor getAccessor(ServiceConnection conn) {
        return new ManagedObjectAccessor(conn);
    }
    
    public VimPortType getVimPort() {
        return vimPort;
    }

    public void setVimPort(VimPortType vimPort) {
        this.vimPort = vimPort;
    }
    
    public ManagedObjectAccessor getMoa() {
        return moa;
    }

    public void setMoa(ManagedObjectAccessor moa) {
        this.moa = moa;
    }

    public ManagedObjectReference getGuestOpManger() {
        return guestOpManger;
    }

    public void setGuestOpManger(ManagedObjectReference guestOpManger) {
        this.guestOpManger = guestOpManger;
    }

    public ManagedObjectReference getFileManagerRef() {
        return fileManagerRef;
    }

    public void setFileManagerRef(ManagedObjectReference fileManagerRef) {
        this.fileManagerRef = fileManagerRef;
    }

    public ManagedObjectReference getProcessManagerRef() {
        return processManagerRef;
    }

    public void setProcessManagerRef(ManagedObjectReference processManagerRef) {
        this.processManagerRef = processManagerRef;
    }


}
