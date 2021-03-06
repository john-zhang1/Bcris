/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.browse.BrowseDSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.xml.sax.SAXException;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.log4j.Logger;

import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.core.LogManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.app.bulkedit.MetadataExport;

/**
 *
 * AbstractReader that generates a CSV of item, collection
 * or community metadata using MetadataExport
 *
 * @author Kim Shepherd
 */

public class MetadataExportReader extends AbstractReader implements Recyclable
{

     /**
     * Messages to be sent when the user is not authorized to view 
     * a particular bitstream. They will be redirected to the login
     * where this message will be displayed.
     */
	private static final String AUTH_REQUIRED_HEADER = "xmlui.ItemExportDownloadReader.auth_header";
	private static final String AUTH_REQUIRED_MESSAGE = "xmlui.ItemExportDownloadReader.auth_message";
	
    /**
     * How big a buffer should we use when reading from the bitstream before
     * writing to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a download expire in milliseconds. This should be set to
     * some low value just to prevent someone hitting DSpace repeatedly from
     * killing the server. Note: there are 60000 milliseconds in a minute.
     * 
     * Format: minutes * seconds * milliseconds
     */
    protected static final int expires = 60 * 60 * 60000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    private static Logger log = Logger.getLogger(MetadataExportReader.class);

    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();


    DSpaceCSV csv = null;
    MetadataExport exporter = null;
    String filename = null;
    /**
     * Set up the export reader.
     * 
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);

            if(authorizeService.isAdmin(context))
            {

            /* Get our parameters that identify the item, collection
             * or community to be exported
             *
             */

            String handle = par.getParameter("handle");
            DSpaceObject dso = handleService.resolveToObject(context, handle);
            
            java.util.List<Item> itemmd = new ArrayList<>();
            if(dso.getType() == Constants.ITEM)
            {
               itemmd.add(itemService.find(context, dso.getID()));
               List<BrowseDSpaceObject> bdo = new ArrayList<>();
               for(Item item : itemmd) {            	   
            	   bdo.add(new BrowseDSpaceObject(context, item));
               }
               exporter = new MetadataExport(context, bdo.iterator(), false);
            }
            else if(dso.getType() == Constants.COLLECTION)
            {
               Collection collection = (Collection)dso;
               Iterator<Item> toExport = itemService.findByCollection(context, collection);
               List<BrowseDSpaceObject> bdo = new ArrayList<>();
               while(toExport.hasNext()) {            	   
            	   bdo.add(new BrowseDSpaceObject(context, toExport.next()));
               }
               exporter = new MetadataExport(context, bdo.iterator(), false);
            }
            else if(dso.getType() == Constants.COMMUNITY)
            {
               exporter = new MetadataExport(context, (Community)dso, false);
            }

            log.info(LogManager.getHeader(context, "metadataexport", "exporting_handle:" + handle));
            csv = exporter.export();
            filename = handle.replaceAll("/", "-") + ".csv";
            log.info(LogManager.getHeader(context, "metadataexport", "exported_file:" + filename));
            }
            else {
                    /*
                     * Auth should be done by MetadataExport -- pass context through
                     * we should just be catching exceptions and displaying errors here
                     *
                     */

                   if(AuthenticationUtil.isLoggedIn(request)) {
                      String redictURL = request.getContextPath() + "/restricted-resource";
                        HttpServletResponse httpResponse = (HttpServletResponse)
            		objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            		httpResponse.sendRedirect(redictURL);
            		return;
                   }
                   else {

                        String redictURL = request.getContextPath() + "/login";
                        AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);
            		HttpServletResponse httpResponse = (HttpServletResponse)
            		objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            		httpResponse.sendRedirect(redictURL);
            		return;
                   }

            }
            
        }
        catch (RuntimeException e)
        {
            throw e;    
        }
        catch (Exception e)
        {
            throw new ProcessingException("Unable to read bitstream.",e);
        } 
    }

    
    /**
	 * Write the CSV.
	 * 
	 */
    public void generate() throws IOException, SAXException,
            ProcessingException
    {

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition","attachment; filename=" + filename);
 
        out.write(csv.toString().getBytes("UTF-8"));
        out.flush();
        out.close();

        
    }

    
    /**
	 * Recycle
	 */
    public void recycle() {        
        this.response = null;
        this.request = null;
        this.exporter = null;
        this.filename = null;
        this.csv = null;
        super.recycle();
    }


}
