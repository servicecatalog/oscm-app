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
import org.oscm.app.vmware.parser.model.DistributedVirtualSwitch;
import org.oscm.app.vmware.persistence.DataAccessService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.sql.DataSource;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "javax.script.*", "jdk.internal.reflect.*"})
@PrepareForTest({DistributedVirtualSwitchImporter.class, XMLHelper.class})
public class DistributedVirtualSwitchImporterTest {

  private DataAccessService das;
  private DistributedVirtualSwitchImporter dvsImporter;
  private DataSource dataSource;
  private Connection connection;
  private PreparedStatement preparedStatement;
  private ResultSet resultSet;
  private DistributedVirtualSwitch dvs;

  @Before
  public void setUpStreams() throws Exception {
    das = mock(DataAccessService.class);
    dvsImporter = PowerMockito.spy(new DistributedVirtualSwitchImporter(das));
    dataSource = mock(DataSource.class);
    connection = mock(Connection.class);
    preparedStatement = mock(PreparedStatement.class);
    resultSet = mock(ResultSet.class);
    dvs = mock(DistributedVirtualSwitch.class);

    when(das.getDatasource()).thenReturn(dataSource);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);
  }


  @Test
  public void testSaveExecute() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    PowerMockito.mockStatic(XMLHelper.class);
    // when
    Whitebox.invokeMethod(dvsImporter, "save", dvs);
    // then
    verify(preparedStatement, times(2)).setString(1, null);
    verify(preparedStatement, times(2)).setString(2, null);
    verify(preparedStatement, times(1)).execute();
  }

  @Test
  public void testGetClusterKey() throws Exception {
    // given
    when(resultSet.next()).thenReturn(true);
    // when
    int result = Whitebox.invokeMethod(dvsImporter, "getClusterKey", "", "", "");
    // then
    assertEquals(0, result);
  }

  @Test(expected = Exception.class)
  public void testGetClusterKeyThrowException() throws Exception {
    // when
    Whitebox.invokeMethod(dvsImporter, "getClusterKey", "", "");
  }

  @Test
  public void testLoad() throws Exception {
    // given
    final File initialFile = new File("resources/csv/distributedvirtualswitch.csv");
    final InputStream csvFile =
        new DataInputStream(new FileInputStream(initialFile));
    when(resultSet.next()).thenReturn(true);
    // when
    dvsImporter.load(csvFile);
    // then
    PowerMockito.verifyPrivate(dvsImporter, times(1)).invoke("save", any());
    verify(preparedStatement, times(1)).setString(1, "esscluster");
    verify(preparedStatement, times(1)).setString(2, "EST");
  }
}
