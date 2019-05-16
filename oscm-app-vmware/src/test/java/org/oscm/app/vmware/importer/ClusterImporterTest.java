/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2019
 *
 *  Creation Date: 2019-02-01
 *
 *******************************************************************************/

package org.oscm.app.vmware.importer;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class ClusterImporterTest {

    final String testXML = ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<ess:essvcenter xmlns:ess=\"http://oscm.org/xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://oscm.org/xsd ../../oscm-app-vmware\\resources\\XSD\\Loadbalancer_schema.xsd\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionHostBalancer\" cpuWeight=\"0.5\" memoryWeight=\"1\" vmWeight=\"1\"/>"
            + "<host enabled=\"true\" name=\"estvmwdev1.intern.est.fujitsu.com\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
            + "</host>"
            + "<host enabled=\"true\" name=\"estvmwdev2.intern.est.fujitsu.com\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
            + "</host>"
            + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev0\"/>"
            + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev1\"/>"
            + "</ess:essvcenter>";

    final String testWrongXML = ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<ess:essvcenter xmlns:ess=\"http://oscm.org/xsd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://oscm.org/xsd ../../oscm-app-vmware\\resources\\XSD\\Loadbalancer_schema.xsd\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionHostBalancer\" cpuWeight=\"0.5\" memoryWeight=\"1\" vmWeight=\"1\"/>"
            + "<host enabled=\"true\" name=\"estvmwdev1.intern.est.fujitsu.com\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
            + "</host>"
            + "<host enabled=\"true\" name=\"estvmwdev2.intern.est.fujitsu.com\">"
            + "<balancer class=\"org.oscm.app.vmware.business.balancer.EquipartitionStorageBalancer\" storage=\"VMdev0,VMdev1\"/>"
            + "</host>"
            + "<storage enabled=\"true\" limit=\"85%\" name=\"VMdev0\"/>"
            + "<storage enabled=\"true\" limit=\"85%\"/>" + "</ess:essvcenter>";

    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setErr(originalErr);
    }

    private File getSource(String filename) throws Exception {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource("META-INF/" + filename);
        File file = new File(url.toURI());
        return file;
    }

    @Test
    public void testValidSchema() throws Exception {

        // given
        File file = getSource("Loadbalancer_schema.xsd");
        String constant = XMLConstants.W3C_XML_SCHEMA_NS_URI;
        SchemaFactory xsdFactory = SchemaFactory.newInstance(constant);
        Schema schema = xsdFactory.newSchema(file);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);

        // when
        dfactory.setValidating(false);
        dfactory.setIgnoringElementContentWhitespace(true);
        dfactory.setSchema(schema);
        DocumentBuilder builder = dfactory.newDocumentBuilder();
        Document doc = builder
                .parse(new InputSource(new StringReader(testXML)));

        // then
    }

    @Test
    public void testInvalidSchema()
            throws Exception {

        // given
        File file = getSource("Loadbalancer_schema.xsd");
        String constant = XMLConstants.W3C_XML_SCHEMA_NS_URI;
        SchemaFactory xsdFactory = SchemaFactory.newInstance(constant);
        Schema schema = xsdFactory.newSchema(file);
        DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(true);

        // when
        dfactory.setValidating(false);
        dfactory.setIgnoringElementContentWhitespace(true);
        dfactory.setSchema(schema);
        DocumentBuilder builder = dfactory.newDocumentBuilder();
        Document doc = builder
                .parse(new InputSource(new StringReader(testWrongXML)));

        // then
        assertTrue(errContent.toString()
                .contains("Attribute 'name' must appear on element 'storage'"));
    }

}
