/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;

/**
 * Implements virtual field processing to build custom identifier
 * 
 * @author Jordan Piščanc (piscanc at units dot it)
 */
public class VirtualFieldCruiIdentifier
    implements VirtualFieldDisseminator, VirtualFieldIngester
{

    public VirtualFieldCruiIdentifier()
    {
    }

    public String[] getMetadata(Item item, Map fieldCache, String fieldName)
    {
        if(fieldCache.containsKey(fieldName))
            return (new String[] {
                (String)fieldCache.get(fieldName)
            });
        List<IMetadataValue> mds = item.getMetadata("dc", "identifier", "issn", "*");
        String element = "";
        if(mds != null && mds.size() > 0)
            element = mds.get(0).getValue();
        mds = item.getMetadata("dc", "identifier", "eissn", "*");
        if(mds != null && mds.size() > 0)
            element = mds.get(0).getValue();
        String handle = item.getHandle();
        if(StringUtils.isNotBlank(handle))
            fieldCache.put("virtual.cruiidentifier", (new StringBuilder(String.valueOf(element))).append("/").append(handle.substring(6)).toString());
        else
            fieldCache.put("virtual.cruiidentifier", (new StringBuilder("ATT-")).append(item.getID()).toString());
        if(fieldCache.containsKey(fieldName))
            return (new String[] {
                (String)fieldCache.get(fieldName)
            });
        else
            return null;
    }

    public boolean addMetadata(Item item, Map fieldCache, String fieldName, String s)
    {
        // NOOP - we won't add any metadata yet, we'll pick it up when we
        // finalise the item
        return true;
    }

    public boolean finalizeItem(Item item, Map fieldCache)
    {
        return false;
    }
}
