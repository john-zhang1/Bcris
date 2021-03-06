/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public class LastLoggedInAction implements PostLoggedInAction {

	@Override
	public void loggedIn(Context context, HttpServletRequest request,
			EPerson eperson) {
		try
		{
			eperson.setLastActive(new Date());
			eperson.getDSpaceObjectService().update(context, eperson);
			context.commit();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
