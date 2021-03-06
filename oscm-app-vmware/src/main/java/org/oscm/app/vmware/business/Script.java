/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *  Creation Date: 2016-05-24
 *
 *******************************************************************************/

package org.oscm.app.vmware.business;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.io.IOUtils;
import org.oscm.app.v2_0.exceptions.APPlatformException;
import org.oscm.app.vmware.business.Script.OS;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.oscm.app.vmware.remote.bes.ServiceParamRetrieval;
import org.oscm.app.vmware.remote.vmware.ManagedObjectAccessor;
import org.oscm.app.vmware.remote.vmware.ServiceConnection;
import org.oscm.app.vmware.remote.vmware.VMwareClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vmware.vim25.FileFaultFaultMsg;
import com.vmware.vim25.FileTransferInformation;
import com.vmware.vim25.GuestOperationsFaultFaultMsg;
import com.vmware.vim25.GuestPosixFileAttributes;
import com.vmware.vim25.GuestProcessInfo;
import com.vmware.vim25.GuestProgramSpec;
import com.vmware.vim25.GuestWindowsFileAttributes;
import com.vmware.vim25.InvalidStateFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.NamePasswordAuthentication;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.TaskInProgressFaultMsg;
import com.vmware.vim25.VimPortType;

public class Script {

    private static final Logger LOG = LoggerFactory.getLogger(Script.class);

    private static final String WINDOWS_GUEST_FILE_PATH = "C:\\Windows\\Temp\\runonce.bat";
    private static final String LINUX_GUEST_FILE_PATH = "/tmp/runonce.sh";
    static final String HIDDEN_PWD = "*****";
    
    private boolean scriptExecuting = false;

    private OS os;
    private VMPropertyHandler ph;
    private ServiceParamRetrieval sp;

    private String guestUserId;
    private String guestPassword;
    private String executableScript;
    
    private static Script script = null;
    
    private Script() {};
    
    public static synchronized Script getInstance () {
        if (script == null) {
            script = new Script();
        }
        return script;
      }
    
    
    public void initScript(VMPropertyHandler ph, OS os) throws Exception {
        initializeScript(ph, os);

        executableScript = downloadFile(ph
                .getServiceSetting(VMPropertyHandler.TS_SCRIPT_URL));
    }
    
    public void initScript(VMPropertyHandler ph, OS os, String script) throws Exception {
        initializeScript(ph, os);

        this.executableScript = script;
    }

    private void initializeScript(VMPropertyHandler ph, OS os) throws Exception {
        this.ph = ph;
        this.os = os;

        sp = createServiceParameterRetrieval(ph);
        guestUserId = ph.getServiceSetting(VMPropertyHandler.TS_SCRIPT_USERID);
        guestPassword = ph.getServiceSetting(VMPropertyHandler.TS_SCRIPT_PWD);

        // TODO load certificate from vSphere host and install somehow
        disableSSL();
    }

    protected ServiceParamRetrieval createServiceParameterRetrieval(VMPropertyHandler ph) {
        return new ServiceParamRetrieval(ph);
    }
    
    public enum OS {
        LINUX("\n"), WINDOWS("\r\n");

        private String lineEnding;

        private OS(String lineEnding) {
            this.lineEnding = lineEnding;
        }

        public String getLineEnding() {
            return lineEnding;
        }
    }

    private class TrustAllTrustManager implements javax.net.ssl.TrustManager,
            javax.net.ssl.X509TrustManager {

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @SuppressWarnings("unused")
        public boolean isClientTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        @Override
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        @Override
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }
    }

    /**
     * Declare a host name verifier that will automatically enable the
     * connection. The host name verifier is invoked during the SSL handshake.
     */
    void disableSSL() throws Exception {
        javax.net.ssl.HostnameVerifier verifier = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };

        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager trustManager = new TrustAllTrustManager();
        trustAllCerts[0] = trustManager;

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                .getInstance("SSL");
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);

        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
                .getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(verifier);
    }

    String downloadFile(String url) throws Exception {
        HttpURLConnection conn = null;
        int returnErrorCode = HttpURLConnection.HTTP_OK;
        StringWriter writer = new StringWriter();
        try {
            URL urlSt = new URL(url);
            conn = (HttpURLConnection) urlSt.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestMethod("GET");
            try (InputStream in = conn.getInputStream();) {
                IOUtils.copy(in, writer, "UTF-8");
            }
            returnErrorCode = conn.getResponseCode();
        } catch (Exception e) {
            LOG.error("Failed to download script file " + url, e);
            throw new Exception("Failed to download script file " + url);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if (HttpURLConnection.HTTP_OK != returnErrorCode) {
            throw new Exception("Failed to download script file " + url);
        }

        return writer.toString();
    }

    protected void uploadScriptFileToVM(VimPortType vimPort,
            ManagedObjectReference vmwInstance,
            ManagedObjectReference fileManagerRef,
            NamePasswordAuthentication auth, String script, String hostname)
            throws Exception {

        String fileUploadUrl = null;
        if (os == OS.WINDOWS) {
            GuestWindowsFileAttributes guestFileAttributes = new GuestWindowsFileAttributes();
            guestFileAttributes.setAccessTime(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new GregorianCalendar()));
            guestFileAttributes.setModificationTime(DatatypeFactory
                    .newInstance().newXMLGregorianCalendar(
                            new GregorianCalendar()));
            fileUploadUrl = vimPort.initiateFileTransferToGuest(fileManagerRef,
                    vmwInstance, auth, WINDOWS_GUEST_FILE_PATH,
                    guestFileAttributes, script.length(), true);
        } else {
            GuestPosixFileAttributes guestFileAttributes = new GuestPosixFileAttributes();
            guestFileAttributes.setPermissions(Long.valueOf(500));
            guestFileAttributes.setAccessTime(DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(new GregorianCalendar()));
            guestFileAttributes.setModificationTime(DatatypeFactory
                    .newInstance().newXMLGregorianCalendar(
                            new GregorianCalendar()));
            fileUploadUrl = vimPort.initiateFileTransferToGuest(fileManagerRef,
                    vmwInstance, auth, LINUX_GUEST_FILE_PATH,
                    guestFileAttributes, script.length(), true);
        }

        fileUploadUrl = fileUploadUrl.replaceAll("\\*", hostname);
        LOG.debug("Uploading the file to :" + fileUploadUrl);

        HttpURLConnection conn = null;
        int returnErrorCode = HttpURLConnection.HTTP_OK;

        try {
            URL urlSt = new URL(fileUploadUrl);
            conn = (HttpURLConnection) urlSt.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Length",
                    Long.toString(script.length()));
            try (OutputStream out = conn.getOutputStream();) {
                out.write(script.getBytes());
            }
            returnErrorCode = conn.getResponseCode();
        } catch (Exception e) {
            LOG.error("Failed to upload file.", e);
            throw new Exception("Failed to upload file.");
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        if (HttpURLConnection.HTTP_OK != returnErrorCode) {
            throw new Exception("Failed to upload file. HTTP response code: "
                    + returnErrorCode);
        }
    }

    private String insertServiceParameter() throws Exception {
        
        String logExecutableScript = executableScript;
        
        if (isUpdatingLinuxPassword()) {
            logExecutableScript = hideScriptPasswords(executableScript, 
                    ph.getServiceSetting(VMPropertyHandler.TS_LINUX_ROOT_PWD));
        } 
        
        LOG.debug("Script before patching:\n" + logExecutableScript);

        String firstLine = executableScript.substring(0,
                executableScript.indexOf(os.getLineEnding()));
        String rest = executableScript.substring(executableScript.indexOf(os.getLineEnding()) + 1,
                executableScript.length());

        StringBuffer sb = new StringBuffer();
        List<String> passwords = new ArrayList<String>();
        if (os == OS.WINDOWS) {
            passwords = addServiceParametersForWindowsVms(sb);
        } else {
            passwords = addServiceParametersForLinuxVms(sb);
        }
        List<String> scriptPasswords = addOsIndependetServiceParameters(sb);
        passwords.addAll(scriptPasswords);

        String patchedScript;
        if (os == OS.WINDOWS) {
            patchedScript = sb.toString() + os.getLineEnding() + firstLine
                    + os.getLineEnding() + rest;
        } else {
            patchedScript = firstLine + os.getLineEnding() + sb.toString()
                    + os.getLineEnding() + rest;
        }
        String logPatchedScript ="";
        if (isUpdatingLinuxPassword()) {
            logPatchedScript = hideScriptPasswords(patchedScript, 
                    ph.getServiceSetting(VMPropertyHandler.TS_LINUX_ROOT_PWD));
            logPatchedScript = hidePasswords(logPatchedScript, passwords, os);
        } else {
            logPatchedScript = hidePasswords(patchedScript, passwords, os);
        }
            
        LOG.debug("Patched script:\n" + logPatchedScript);
        return patchedScript;
    }

    static String hideScriptPasswords (String script, String newPassword) {
        return script.replace(newPassword, "'" + HIDDEN_PWD + "'");
    }

    static String hidePasswords(String script, List<String> passwords, OS os) {
        final String pwdPrefix = "_PWD=";
        String logScript = script;
        for (String password : passwords) {
            if (OS.LINUX.equals(os)) {
                logScript = logScript.replace(pwdPrefix + "'" + password + "'",
                        pwdPrefix + "'" + HIDDEN_PWD + "'");
            } else if (OS.WINDOWS.equals(os)) {
                logScript = logScript.replace(
                        pwdPrefix + password + os.getLineEnding(), pwdPrefix
                                + HIDDEN_PWD + os.getLineEnding());
            }
        }
        return logScript;
    }

    private List<String> addServiceParametersForWindowsVms(StringBuffer sb)
            throws Exception, APPlatformException {
        List<String> passwords = new ArrayList<String>();
        passwords
                .add(sp.getServiceSetting(VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD));
        passwords
                .add(sp.getServiceSetting(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_WINDOWS_DOMAIN_ADMIN_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_WINDOWS_DOMAIN_JOIN));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_DOMAIN_NAME));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_WINDOWS_WORKGROUP));
        return passwords;
    }

    private List<String> addServiceParametersForLinuxVms(StringBuffer sb)
            throws Exception, APPlatformException {
        List<String> passwords = new ArrayList<String>();
        passwords
                .add(sp.getServiceSetting(VMPropertyHandler.TS_LINUX_ROOT_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_LINUX_ROOT_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_DOMAIN_NAME));
        return passwords;
    }

    protected List<String> addOsIndependetServiceParameters(StringBuffer sb)
            throws Exception {
        List<String> passwords = new ArrayList<String>();
        sb.append(buildParameterCommand(VMPropertyHandler.TS_INSTANCENAME,
                ph.getInstanceName()));
        sb.append(buildParameterCommand(VMPropertyHandler.REQUESTING_USER));
        passwords = addScriptParameters(sb);
        addNetworkServiceParameters(sb);
        addDataDiskParameters(sb);
        return passwords;
    }

    List<String> addScriptParameters(StringBuffer sb) throws Exception {
        List<String> passwords = new ArrayList<String>();
        passwords.add(sp.getServiceSetting(VMPropertyHandler.TS_SCRIPT_PWD));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_SCRIPT_URL));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_SCRIPT_USERID));
        sb.append(buildParameterCommand(VMPropertyHandler.TS_SCRIPT_PWD));
        return passwords;
    }

    private void addDataDiskParameters(StringBuffer sb) throws Exception {
        for (String key : ph.getDataDiskMountPointParameterKeys()) {
            sb.append(buildParameterCommand(key));
        }
        for (String key : ph.getDataDiskSizeParameterKeys()) {
            sb.append(buildParameterCommand(key));
        }
    }

    private void addNetworkServiceParameters(StringBuffer sb) throws Exception {
        int numNics = Integer.parseInt(sp
                .getServiceSetting(VMPropertyHandler.TS_NUMBER_OF_NICS));
        while (numNics > 0) {
            String param = getIndexedParam(
                    VMPropertyHandler.TS_NIC1_DNS_SERVER, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(VMPropertyHandler.TS_NIC1_DNS_SUFFIX,
                    numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(VMPropertyHandler.TS_NIC1_GATEWAY, numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(VMPropertyHandler.TS_NIC1_IP_ADDRESS,
                    numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(VMPropertyHandler.TS_NIC1_NETWORK_ADAPTER,
                    numNics);
            sb.append(buildParameterCommand(param));

            param = getIndexedParam(VMPropertyHandler.TS_NIC1_SUBNET_MASK,
                    numNics);
            sb.append(buildParameterCommand(param));

            numNics--;
        }
    }

    private String buildParameterCommand(String key) throws Exception {
        return buildParameterCommand(key, sp.getServiceSetting(key));
    }

    private String buildParameterCommand(String key, String value) {
        switch (os) {
        case LINUX:
            return key + "='" + value + "'" + os.getLineEnding();
        case WINDOWS:
            return "set " + key + "=" + value + os.getLineEnding();
        default:
            throw new IllegalStateException("OS type" + os.name()
                    + " not supported by Script execution");
        }
    }

    private String getIndexedParam(String param, int index) {
        return param.replace('1', Integer.toString(index).charAt(0));
    }
    
    public void execute(VMwareClient vmw, ManagedObjectReference vmwInstance)
            throws Exception {
        if (!scriptExecuting) {
            executeScript(vmw, vmwInstance);
        }
    }

    private void executeScript(VMwareClient vmw,
            ManagedObjectReference vmwInstance) throws Exception {
        try {
            setScriptExecuting(true);
            LOG.debug("");

            ScriptExecutionObjectReferences objectRef = getScriptExecutionObjectReferences(
                    vmw);
            NamePasswordAuthentication auth = getPasswordAuthentication();
            String scriptPatched = insertServiceParameter();
            URL vSphereURL = getVSphereURL();

            uploadScriptFileToVM(objectRef.getVimPort(), vmwInstance, objectRef.getFileManagerRef(), auth,
                    scriptPatched, vSphereURL.getHost());
            LOG.debug("Executing CreateTemporaryFile guest operation");
            
            String tempFilePath = objectRef.getVimPort().createTemporaryFileInGuest(
                    objectRef.getFileManagerRef(), vmwInstance, auth, "", "", "");
            LOG.debug("Successfully created a temporary file at: "
                    + tempFilePath + " inside the guest");
            GuestProgramSpec spec = getGuestProgramSpec(tempFilePath);
            LOG.debug("Starting the specified program inside the guest");
            long pid = objectRef.getVimPort().startProgramInGuest(objectRef.getProcessManagerRef(),
                    vmwInstance, auth, spec);
            LOG.debug("Process ID of the program started is: " + pid + "");

            if (isUpdatingLinuxPassword()) {
                setUpdatedPassword(auth);
            }

            List<GuestProcessInfo> procInfo = null;
            List<Long> pidsList = new ArrayList<Long>();
            pidsList.add(Long.valueOf(pid));
            
            do {
                LOG.debug("Waiting for the process to finish running.");
                try {
                    procInfo = getProcInfo(vmwInstance, objectRef.getVimPort(), objectRef.getProcessManagerRef(), auth,
                            pidsList);
                } catch (Exception e) {
                    setNewAuthPassword(auth, e);
                }
                Thread.sleep(1000);
            } while (procInfo != null && procInfo.get(0).getEndTime() == null);

            checkIfScriptWasRunningSuccessful(vmwInstance, objectRef.getVimPort(),
                    objectRef.getFileManagerRef(), auth, vSphereURL, tempFilePath, procInfo);
            setScriptExecuting(false);
            
        } catch (Exception e) {
            setScriptExecuting(false);
            throw e;
        }
    }

    protected ScriptExecutionObjectReferences getScriptExecutionObjectReferences(
            VMwareClient vmw) throws Exception {
        ScriptExecutionObjectReferences  objectRef = new ScriptExecutionObjectReferences(vmw);
        return objectRef;
    }

    private void checkIfScriptWasRunningSuccessful(
            ManagedObjectReference vmwInstance, VimPortType vimPort,
            ManagedObjectReference fileManagerRef,
            NamePasswordAuthentication auth, URL vSphereURL,
            String tempFilePath, List<GuestProcessInfo> procInfo)
            throws FileFaultFaultMsg, GuestOperationsFaultFaultMsg,
            InvalidStateFaultMsg, RuntimeFaultFaultMsg, TaskInProgressFaultMsg,
            Exception {
        if (procInfo != null
                && procInfo.get(0).getExitCode().intValue() != 0) {
            LOG.error(
                    "Script return code: " + procInfo.get(0).getExitCode());
            FileTransferInformation fileTransferInformation = null;
            fileTransferInformation = vimPort.initiateFileTransferFromGuest(
                    fileManagerRef, vmwInstance, auth, tempFilePath);
            String fileDownloadUrl = fileTransferInformation.getUrl()
                    .replaceAll("\\*", vSphereURL.getHost());
            LOG.debug(
                    "Downloading the output file from :" + fileDownloadUrl);
            String scriptOutput = downloadFile(fileDownloadUrl);
            LOG.error("Script execution output: " + scriptOutput);
        }
    }

    private void setNewAuthPassword(NamePasswordAuthentication auth,
            Exception e) {
        LOG.warn(
                "listProcessesInGuest() failed. setting new Linux root password for authentication",
                e);
        if (os == OS.WINDOWS) {
            auth.setPassword(ph.getServiceSetting(
                    VMPropertyHandler.TS_WINDOWS_LOCAL_ADMIN_PWD));
        } else {
            auth.setPassword(ph.getServiceSetting(
                    VMPropertyHandler.TS_LINUX_ROOT_PWD));
        }
    }

    protected GuestProgramSpec getGuestProgramSpec(String tempFilePath) {
        GuestProgramSpec spec = new GuestProgramSpec();

        if (os == OS.WINDOWS) {
            spec.setProgramPath(WINDOWS_GUEST_FILE_PATH);
            spec.setArguments(" > " + tempFilePath);
        } else {
            spec.setProgramPath(LINUX_GUEST_FILE_PATH);
            spec.setArguments(" > " + tempFilePath + " 2>&1");
        }
        return spec;
    }

    protected NamePasswordAuthentication getPasswordAuthentication() {
        NamePasswordAuthentication auth = new NamePasswordAuthentication();
        auth.setUsername(guestUserId);
        auth.setPassword(guestPassword);
        auth.setInteractiveSession(false);
        return auth;
    }

    private void setUpdatedPassword(NamePasswordAuthentication auth)
            throws InterruptedException {
        auth.setUsername(guestUserId);
        auth.setPassword(ph.getServiceSetting(
                VMPropertyHandler.TS_LINUX_ROOT_PWD));
        guestPassword = ph
                .getServiceSetting(VMPropertyHandler.TS_LINUX_ROOT_PWD);
        ph.setServiceSetting(
                ph.getServiceSetting(VMPropertyHandler.TS_SCRIPT_PWD),
                ph.getServiceSetting(
                        VMPropertyHandler.TS_LINUX_ROOT_PWD));
        Thread.sleep(1000);
    }

    private boolean isUpdatingLinuxPassword() {
        return "UPDATE_LINUX_PASSWORD"
                .equals(ph.getServiceSetting(VMPropertyHandler.SM_STATE));
    }

    protected List<GuestProcessInfo> getProcInfo(
            ManagedObjectReference vmwInstance, VimPortType vimPort,
            ManagedObjectReference processManagerRef,
            NamePasswordAuthentication auth, List<Long> pidsList)
            throws GuestOperationsFaultFaultMsg, InvalidStateFaultMsg,
            RuntimeFaultFaultMsg, TaskInProgressFaultMsg {
        return vimPort.listProcessesInGuest(processManagerRef,
                vmwInstance, auth, pidsList);
    }

    protected URL getVSphereURL()
            throws MalformedURLException, Exception {
        String vcenter = ph.getServiceSetting(
                VMPropertyHandler.TS_TARGET_VCENTER_SERVER);
        DataAccessService das = new DataAccessService(ph.getLocale());
        return new URL(das.getCredentials(vcenter).getURL());
    }



    public boolean isScriptExecuting() {
        return scriptExecuting;
    }

    public void setScriptExecuting(boolean scriptIsExecuted) {
        this.scriptExecuting = scriptIsExecuted;
    }
}
