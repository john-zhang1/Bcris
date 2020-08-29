/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.authority.indexer.AuthorityIndexingService;
import org.dspace.authority.service.AuthorityService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Service implementation for the Metadata Authority
 * This class is responsible for all business logic calls for the Metadata Authority and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class AuthorityServiceImpl implements AuthorityService{


    @Autowired(required = true)
    protected AuthorityIndexingService indexingService;
    
    protected List<AuthorityIndexerInterface> indexers;

    protected AuthorityServiceImpl()
    {

    }

    @Override
    public void indexItem(Context context, Item item) throws SQLException, AuthorizeException {
        if(!isConfigurationValid()){
            //Cannot index, configuration not valid
            return;
        }

        for (AuthorityIndexerInterface indexerInterface : getIndexers()) {
            List<AuthorityValue> authorityValues = indexerInterface.getAuthorityValues(context , item);
            for (AuthorityValue authorityValue : authorityValues) {
                indexingService.indexContent(authorityValue);
            }
        }
        //Commit to our server
        indexingService.commit();
    }

    @Override
    public boolean isConfigurationValid()
    {
        if(!indexingService.isConfiguredProperly()){
            return false;
        }

        for (AuthorityIndexerInterface indexerInterface : getIndexers()) {
            if(!indexerInterface.isConfiguredProperly()){
                return false;
            }
        }
        return true;
    }

    public List<AuthorityIndexerInterface> getIndexers() {
        if(indexers == null) {
            indexers = DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(AuthorityIndexerInterface.class);
        }
        return indexers;
    }

}