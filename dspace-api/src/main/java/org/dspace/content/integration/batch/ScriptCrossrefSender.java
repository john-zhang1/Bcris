/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.batch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.hibernate.Session;
import org.xml.sax.SAXException;

public class ScriptCrossrefSender
{

    private static Validator validator;

    private static final String HOST = ConfigurationManager
            .getProperty("crossref.host");

    private static final int PORT = ConfigurationManager
            .getIntProperty("crossref.port");

    private static final String CROSSREF_ENTRYMODE = ConfigurationManager
            .getProperty("crossref.mode");

    private static final String PASSWORD = ConfigurationManager
            .getProperty("crossref.password");

    private static final String USERNAME = ConfigurationManager
            .getProperty("crossref.username");

    private static String dbName = ConfigurationManager.getProperty("db.name");

    public static String TABLE_NAME_DOI2ITEM = "doi2item";

    private static MultiThreadedHttpConnectionManager m_cManager;

    private static String servicePOST = ConfigurationManager
            .getProperty("crossref.path.deposit");

    /** Seconds to wait before a connection is established. */
    public static int TIMEOUT_SECONDS = 60;

    /** Seconds to wait while waiting for data over the socket (SO_TIMEOUT). */
    public static int SOCKET_TIMEOUT_SECONDS = 1800; // 30 minutes

    /** Maximum http connections per host (for REST calls only). */
    public static int MAX_CONNECTIONS_PER_HOST = 15;

    /** Maximum total http connections (for REST calls only). */
    public static int MAX_TOTAL_CONNECTIONS = 30;

    /** log4j logger */
    private static Logger log = Logger.getLogger(ScriptCrossrefSender.class);

    public static void main(String[] args) throws ParseException
    {
        log.info("#### START Script crossref sender: -----" + new Date()
                + " ----- ####");
        Map<UUID, String> result = new HashMap<UUID, String>();

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption(
                "a",
                "all",
                false,
                "Work on new inserted row, with last_modified equals to null or item update recently");
        options.addOption(
                "s",
                "single",
                true,
                "Work on single item, , with last_modified equals to null or item update recently");

        CommandLine line = parser.parse(options, args);

        if (line.hasOption('h'))
        {
            HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp("ScriptCrossrefSender \n", options);
            System.out
                    .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");

            System.exit(0);
        }

        if (line.hasOption('s') && line.hasOption('a'))
        {
            System.out
                    .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");
            System.out.println("Insert either a or s like parameters");
            log.error("Either a or s like parameters");
            System.exit(1);
        }

        // create xsd validator
        SchemaFactory factory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        String uriSchema = ConfigurationManager
                .getProperty("crosswalk.crossref.schemaLocation");

        Source schemaSource = new StreamSource(uriSchema);

        Schema schema = null;
        try
        {
            schema = factory.newSchema(schemaSource);
        }
        catch (SAXException e2)
        {
            log.error(e2.getMessage(), e2);
        }
        // validator to validate source xml to xsd
        validator = schema.newValidator();

        m_cManager = new MultiThreadedHttpConnectionManager();

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();

            if (line.hasOption('a'))
            {

                int limit = 100;

                List<Object[]> rows = null;

                if ("oracle".equals(dbName))
                {
                    rows = getHibernateSession(context).createSQLQuery(
                                    "select d2i.item_id, criteria, identifier_doi from "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i left join item i on d2i.item_id = i.uuid where d2i.last_modified is null OR d2i.last_modified < i.last_modified"
                                            + " AND ROWNUM <= " + limit).list();
                }
                else
                {
                    rows = getHibernateSession(context).createSQLQuery(
                                    "select d2i.item_id, criteria, identifier_doi from "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i left join item i on d2i.item_id = i.uuid where d2i.last_modified is null OR d2i.last_modified < i.last_modified"
                                            + " LIMIT " + limit).list();
                }

                int offset = 0;

                while (!rows.isEmpty())
                {
                    if (offset > 0)
                    {
                        if ("oracle".equals(dbName))
                        {
                            rows = getHibernateSession(context).createSQLQuery(
                                            "select d2i.item_id, criteria, identifier_doi from "
                                                    + TABLE_NAME_DOI2ITEM
                                                    + " d2i left join item i on d2i.item_id = i.uuid where d2i.last_modified is null OR d2i.last_modified < i.last_modified"
                                                    + " AND ROWNUM > " + limit
                                                    + " AND ROWNUM <= " + (limit + offset))
                            		.list();
                        }
                        else
                        {
                            rows = getHibernateSession(context).createSQLQuery(
                                            "select d2i.item_id, criteria, identifier_doi from "
                                                    + TABLE_NAME_DOI2ITEM
                                                    + " d2i left join item i on d2i.item_id = i.uuid where d2i.last_modified is null OR d2i.last_modified < i.last_modified"
                                                    + " LIMIT " + limit
                                                    + " OFFSET " + offset)
                            		.list();
                        }
                    }
                    offset = limit + offset;
                    for (Object[] row : rows)
                    {
                    	
                        Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString((String)row[0]));
                        String criteria = (String)row[1];
                        String doi = (String)row[2];

                        try
                        {
                            result.putAll(sendToCrossref(context, item,
                                    criteria, doi));
                        }
                        catch (IOException e)
                        {
                            log.error("FOR item: " + item.getID()
                                    + " ERRORMESSAGE: " + e.getMessage(), e);
                        }
                        catch (AuthorizeException e)
                        {
                            log.error("FOR item: " + item.getID()
                                    + " ERRORMESSAGE: " + e.getMessage(), e);
                        }
                        catch (CrosswalkException e)
                        {
                            log.error("FOR item: " + item.getID()
                                    + " ERRORMESSAGE: " + e.getMessage(), e);
                        }

                    }
                    context.commit();
                }
            }
            else
            {
                if (line.hasOption('s'))
                {
                    UUID id = UUID.fromString(line.getOptionValue("s"));
                    List<Object[]> rows = getHibernateSession(context).createSQLQuery(
                                    "SELECT d2i.item_id, criteria, identifier_doi FROM "
                                            + TABLE_NAME_DOI2ITEM
                                            + " d2i left join item i on d2i.item_id = i.item_id where d2i.item_id = :item_id AND (d2i.last_modified is null OR d2i.last_modified < i.last_modified)").addScalar("item_id").addScalar("criteria").addScalar("identifier_doi").setParameter("item_id", id).list();
                    
                    for(Object[] row : rows) {
                    try
                    {
                        Item item = ContentServiceFactory.getInstance().getItemService().find(context, UUID.fromString((String)row[0]));
                    	String criteria = (String)row[1];
                        String doi = (String)row[2];


                        result.putAll(sendToCrossref(context, item, criteria,
                                doi));
                    }
                    catch (IOException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                    catch (AuthorizeException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                    catch (CrosswalkException e)
                    {
                        log.error(
                                "FOR item: " + id + " ERRORMESSAGE: "
                                        + e.getMessage(), e);
                    }
                    }
                }
                else
                {
                    System.out
                            .println("\n\nUSAGE:\n ScriptCrossrefSender -a|-s <item_id>] \n");
                    System.out.println("Option a or s is needed");
                    log.error("Option a or s is needed");
                    System.exit(1);
                }

            }

        }
        catch (SQLException e1)
        {
            log.error(e1.getMessage(), e1);
            if (context.isValid())
            {
                context.abort();
            }
        }

        log.info("#### Import details ####");

        for (UUID key : result.keySet())
        {
            log.info("ITEM: " + key + " RESULT: " + result.get(key));
        }

        log.info("#### ########################### ####");
        log.info("#### END: -----" + new Date() + " ----- ####");

        System.exit(0);
    }

    private static Map<UUID, String> sendToCrossref(Context context,
            Item item, String criteria, String doi) throws CrosswalkException,
            IOException, SQLException, AuthorizeException
    {

        final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) CoreServiceFactory.getInstance().getPluginService()
                .getNamedPlugin(StreamDisseminationCrosswalk.class, criteria);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCrosswalkDefault.disseminate(context, item, out);

        File file = File.createTempFile("doi_",
                DateFormatUtils.format(new Date(), "dd-MM-yyyy_HH_MM_SS_sss"));
        FileOutputStream fos = new FileOutputStream(file);
        out.writeTo(fos);
        fos.close();

        return send(context, item, file, "DEPOSIT", doi);

    }

    protected static Map<UUID, String> send(Context context, Item target,
            File targetFile, String option, String doi) throws SQLException
    {
        PostMethod post = null;
        Map<UUID, String> result = new HashMap<UUID, String>();

        if (option.equals("DEPOSIT"))
            option = "doMDUpload";
        if (option.equals("DEPOSIT_REFS"))
            option = "doDOICitUpload";
        if (option.equals("QUERY"))
            option = "doQueryUpload";
        if (option.equals("DOIQUERY"))
            option = "doDOIQueryUpload";

        int responseCode = 0;
        try
        {

            // validate xml
            validator.validate(new StreamSource(targetFile));

            // prepare the post method
            post = new PostMethod(servicePOST + "?login_id=" + USERNAME
                    + "&login_passwd=" + PASSWORD + "&operation=" + option
                    + "&area=" + CROSSREF_ENTRYMODE);

            Part[] parts = { new FilePart(targetFile.getName(), targetFile) };

            post.getParams().setContentCharset("UTF8");
            post.setRequestEntity(new MultipartRequestEntity(parts, post
                    .getParams()));

            HostConfiguration nss = new HostConfiguration();
            nss.setHost(HOST, PORT);

            responseCode = getHttpClient().executeMethod(nss, post);
            result.put(target.getID(),
                    responseCode + " - " + post.getResponseBodyAsString());

        }
        catch (Exception e)
        {
            result.put(target.getID(), responseCode + " - " + e.getMessage());
            log.error(
                    "FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
        }
        finally
        {

            if (post != null)
            {
                post.releaseConnection();
            }

            log.info("FOR item: " + target.getID() + " -> RESPONSECODE: "
                    + responseCode + " MESSAGE:" + result.get(target.getID()));
            if (responseCode == 200)
            {
                // target.addMetadata("dc", "identifier", "doi", null, doi);
                target.getItemService().clearMetadata(context, target, "dc", "utils", "processdoi", Item.ANY);
                target.getItemService().addMetadata(context, target, "dc", "utils", "processdoi", null,
                        "crossref");

                try
                {
                    target.getItemService().update(context, target);
                    context.commit();
                }
                catch (AuthorizeException e)
                {
                    log.error("FOR item: " + target.getID() + " ERRORMESSAGE: "
                            + e.getMessage(), e);
                }

              
                    getHibernateSession(context).createSQLQuery(
                                    "UPDATE "
                                            + TABLE_NAME_DOI2ITEM
                                            + " SET LAST_MODIFIED = :last_modified, RESPONSE_CODE = :response_code, NOTE = :note, FILENAME = :filename WHERE ITEM_ID = :item_id").setParameter("last_modified", 
                                    new Date()).setParameter("response_code", responseCode).setParameter("note", result.get(target.getID())).setParameter("filename", targetFile.getName()).setParameter("item_id", target.getID()).executeUpdate();
              
            }
            else
            {
                getHibernateSession(context).createSQLQuery("UPDATE "
                        + TABLE_NAME_DOI2ITEM
                        + " SET RESPONSE_CODE = :response_code, NOTE = :note WHERE ITEM_ID = :item_id").setParameter("response_code", 
                        responseCode).setParameter("note", result.get(target.getID())).setParameter("item_id", 
                        target.getID());
            }

            context.commit();
        }

        return result;
    }

    public static HttpClient getHttpClient()
    {

        m_cManager.getParams().setDefaultMaxConnectionsPerHost(
                MAX_CONNECTIONS_PER_HOST);
        m_cManager.getParams().setMaxTotalConnections(MAX_TOTAL_CONNECTIONS);
        m_cManager.getParams().setConnectionTimeout(TIMEOUT_SECONDS * 1000);
        m_cManager.getParams().setSoTimeout(SOCKET_TIMEOUT_SECONDS * 1000);

        HttpClient client = new HttpClient(m_cManager);

        return client;
    }
    
    protected static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
