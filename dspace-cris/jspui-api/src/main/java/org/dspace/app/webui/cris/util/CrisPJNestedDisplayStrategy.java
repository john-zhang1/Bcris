/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.dspace.app.cris.model.Project;
import org.dspace.app.cris.model.jdyna.BoxProject;
import org.dspace.app.cris.model.jdyna.TabProject;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.webui.util.IDisplayMetadataValueStrategy;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.utils.DSpace;

public class CrisPJNestedDisplayStrategy implements
        IDisplayMetadataValueStrategy
{

    private DSpace dspace = new DSpace();
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject item,
            boolean disableCrossLinks, boolean emph)
    {
    	return null;
    }
    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph)
    {
        // not used
        return null;
    }
    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, Item item, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        return null;
    }

    @Override
    public String getExtraCssDisplay(HttpServletRequest hrq, int limit,
            boolean b, String browseType, UUID colIdx, String field,
            List<IMetadataValue> metadataArray, BrowsableDSpaceObject browseItem,
            boolean disableCrossLinks, boolean emph)
            throws JspException
    {
        return null;
    }

	@Override
	public String getMetadataDisplay(HttpServletRequest hrq, int limit, boolean viewFull, String browseType,
			UUID colIdx, String field, List<IMetadataValue> metadataArray, IGlobalSearchResult item, boolean disableCrossLinks,
			boolean emph) {
        Project crisObject = (Project)item;
        String[] splitted = field.split("\\.");
        //FIXME apply aspectjproxy???
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
                        ApplicationService.class);
        
        List<BoxProject> box = applicationService.findBoxesByTTP(BoxProject.class, crisObject.getClassTypeNested(), splitted[0]);
        
        for (BoxProject ano : box)
        {
        	List<TabProject> tabs = applicationService.getList(TabProject.class);
        	for(TabProject tab : tabs) {
        		List<BoxProject> mask = applicationService.findPropertyHolderInTab(TabProject.class, tab.getId());
        		if(mask.contains(ano)) {
        			String prefix = ConfigurationManager.getProperty("crispjnested.box." + ano.getShortName() +".prefix");
        			if(prefix==null) {
        				prefix = "#";
        			}
        			return "<a href=\"cris/"+ crisObject.getPublicPath() +"/"+ crisObject.getCrisID() +"/"+tab.getShortName()+".html" + prefix + ano.getShortName() + "\">" + metadataArray.get(0).getValue() +"</a>";		
        		}
        	}
            
        }
        return metadataArray.get(0).getValue();
	}
}

