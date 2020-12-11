/**
 * *****************************************************************************
 *
 * <p>Copyright FUJITSU LIMITED 2020
 *
 * <p>Creation Date: 11 Dec 2020
 *
 * <p>*****************************************************************************
 */

package org.oscm.app.vmware.importer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.oscm.app.vmware.business.balancer.XMLHelper;
import org.oscm.app.vmware.parser.IPPoolParser;
import org.oscm.app.vmware.parser.model.IPPool;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({IPPoolImporter.class, XMLHelper.class})
public class IPPoolImporterTest {

  private DataAccessService das;
  private IPPoolImporter ipPoolImporter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private IPPoolParser ipPoolParser;
  private IPPool ipPool;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    ipPoolImporter = PowerMockito.spy(new IPPoolImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    ipPoolParser = mock(IPPoolParser.class);

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }


  @Test
  public void testSaveExecute() throws Exception {
    // given
    ipPool = new IPPool();
    ipPool.vCenter = "vCenter";
    ipPool.datacenter = "datacenter";
    ipPool.cluster = "cluster";
    ipPool.vlan = "vlan";
    ipPool.ipAddress = "ipAddress";
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(ipPoolImporter, "save", ipPool);
    // then
    verify(preparedStatement, times(1)).setString(1, "vlan");
    verify(preparedStatement, times(1)).setString(2, "cluster");
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetVLANTKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(ipPoolImporter, "getVLANTKey", "", "", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetVLANTKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(ipPoolImporter, "getVLANTKey", "", "", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    ipPool = new IPPool();
    ipPool.vCenter = "vCenter";
    ipPool.datacenter = "datacenter";
    ipPool.cluster = "cluster";
    ipPool.vlan = "vlan";
    ipPool.ipAddress = "ipAddress";
    PowerMockito.whenNew(IPPoolParser.class).withAnyArguments().thenReturn(ipPoolParser);
    when(ipPoolParser.readNextObject()).thenReturn(ipPool, null);
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    ipPoolImporter.load(any());
    // then
    PowerMockito.verifyPrivate(ipPoolImporter, times(1)).invoke("save", ipPool);
    verify(preparedStatement, times(1)).setString(1, "vlan");
    verify(preparedStatement, times(1)).setString(2, "cluster");
  }
}
