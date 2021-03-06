/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.signposting;

import java.beans.PropertyEditor;
import java.text.MessageFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.model.jdyna.RPProperty;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.signposting.ItemSignPostingProcessor;

/**
 * @author Pascarelli Luigi Andrea
 */
public class AuthorItemHomeProcessing implements ItemSignPostingProcessor
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(AuthorItemHomeProcessing.class);

    private String metadataField;

    private String relationHeader;

    private String retrievedExternally;

    private String pattern;

    private ApplicationService applicationService;

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Item item)
            throws PluginException, AuthorizeException
    {

        try
        {
            MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
            if (mam.isAuthorityControlled(
                    getMetadataField().replaceAll("\\.", "_")))
            {
                if (StringUtils.isNotBlank(getRetrievedExternally()))
                {
                    List<IMetadataValue> metadatums = item
                            .getMetadataValueInDCFormat(getMetadataField());
                    for (IMetadataValue metadatum : metadatums)
                    {
                        String authority = metadatum.getAuthority();
                        if (StringUtils.isNotBlank(authority))
                        {
                            ResearcherPage entity = applicationService
                                    .getEntityByCrisId(authority,
                                            ResearcherPage.class);
                            if (entity != null)
                            {
                                List<RPProperty> values = entity
                                        .getAnagrafica4view()
                                        .get(getRetrievedExternally());
                                for (RPProperty val : values)
                                {
                                    PropertyEditor pe = val.getTypo()
                                            .getRendering().getPropertyEditor(
                                                    applicationService);
                                    pe.setValue(val.getObject());
                                    response.addHeader("Link",
                                            MessageFormat.format(getPattern(),
                                                    pe.getAsText()) + "; rel=\""
                                                    + getRelationHeader()
                                                    + "\"");
                                }
                            }
                        }
                    }
                }
                else
                {
                    List<IMetadataValue> metadatums = item
                            .getMetadataValueInDCFormat(getMetadataField());
                    for (IMetadataValue metadatum : metadatums)
                    {
                        if (StringUtils.isNotBlank(metadatum.getAuthority()))
                        {
                            response.addHeader("Link",
                                    MessageFormat.format(getPattern(),
                                            metadatum.getAuthority()) + "; rel=\""
                                            + getRelationHeader() + "\"");
                        }
                    }
                }
            }
            else
            {

                List<IMetadataValue> metadatums = item
                        .getMetadataValueInDCFormat(getMetadataField());
                for (IMetadataValue metadatum : metadatums)
                {
                    if (StringUtils.isNotBlank(metadatum.getValue()))
                    {
                        response.addHeader("Link", metadatum.getValue() + "; rel=\""
                                + getRelationHeader() + "\"");
                    }
                }

            }
        }
        catch (Exception ex)
        {
            log.error("Problem to add signposting pattern", ex);
        }
    }

    public String getMetadataField()
    {
        return metadataField;
    }

    public void setMetadataField(String metadataField)
    {
        this.metadataField = metadataField;
    }

    public String getRelationHeader()
    {
        return relationHeader;
    }

    public void setRelationHeader(String relationHeader)
    {
        this.relationHeader = relationHeader;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    public ApplicationService getApplicationService()
    {
        return applicationService;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }

    public String getRetrievedExternally()
    {
        return retrievedExternally;
    }

    public void setRetrievedExternally(String retrievedExternally)
    {
        this.retrievedExternally = retrievedExternally;
    }

}
