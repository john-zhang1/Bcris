/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.util.Locale;

import org.dspace.browse.BrowsableDSpaceObject;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;

public class TitleWithDigitSignature extends MD5ValueSignature
{

    @Override
    protected String normalize(BrowsableDSpaceObject item, String value)
    {
        if (value != null)
        {

            String norm = Normalizer.normalize(value, Normalizer.NFD);
            CharsetDetector cd = new CharsetDetector();
            cd.setText(value.getBytes());
            CharsetMatch detect = cd.detect();
            if (detect != null && detect.getLanguage() != null)
            {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "")
                        .toLowerCase(new Locale(detect.getLanguage()));
            }
            else
            {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase();
            }
            return norm;
        }
        else
        {
            return "item:" + item.getID();
        }

    }
}
