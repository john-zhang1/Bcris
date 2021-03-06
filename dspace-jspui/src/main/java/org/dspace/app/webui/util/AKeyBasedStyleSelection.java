/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import org.dspace.services.factory.DSpaceServicesFactory;

import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
/**
 * Simple abstract class that provide utility method for get/check style configuration from dspace.cfg file 
 * @author Andrea Bollini
 * @version $Revision$
 *
 */
public abstract class AKeyBasedStyleSelection implements StyleSelection
{   
    public String[] getConfigurationForStyle(Context context, String style, HttpServletRequest request) throws SQLException
    {
    	Locale locale = UIUtil.getSessionLocale(request);
		if (locale != null) {
			String localeStyle = locale.getLanguage() + "." + style;
			String[] config = DSpaceServicesFactory.getInstance().getConfigurationService()
	                .getArrayProperty("webui.itemdisplay." + localeStyle);
			if (ArrayUtils.isNotEmpty(config)) {
				return config;
			}
		}
        return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("webui.itemdisplay." + style);
    }
    
    public boolean isConfigurationDefinedForStyle(Context context, String style, HttpServletRequest request) throws SQLException
    {
    	Locale locale = UIUtil.getSessionLocale(request);
		if (locale != null) {
			String localeStyle = locale.getLanguage() + "." + style;
			String config = DSpaceServicesFactory.getInstance().getConfigurationService()
	                .getProperty("webui.itemdisplay." + localeStyle);
			if (config != null) {
				return true;
			}
		}
		return DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("webui.itemdisplay." + style) != null;
    }
}
