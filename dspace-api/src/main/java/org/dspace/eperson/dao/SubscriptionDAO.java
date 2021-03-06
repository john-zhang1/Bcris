/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Subscription;

/**
 * Database Access Object interface class for the Subscription object.
 * The implementation of this class is responsible for all database calls for the Subscription object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface SubscriptionDAO extends GenericDAO<Subscription> {

    public void deleteByCollection(Context context, Collection collection) throws SQLException;

    public List<Subscription> findByEPerson(Context context, EPerson eperson) throws SQLException;
    public List<Subscription> findByEPersonWithCollection(Context context, EPerson eperson) throws SQLException;
    public List<Subscription> findByEPersonWithCommunity(Context context, EPerson eperson) throws SQLException;
    
    public Subscription findByCollectionAndEPerson(Context context, EPerson eperson, Collection collection) throws SQLException;

    public void deleteByEPerson(Context context, EPerson eperson) throws SQLException;
    public void deleteByEPersonWithCollection(Context context, EPerson eperson) throws SQLException;
    public void deleteByEPersonWithCommunity(Context context, EPerson eperson) throws SQLException;
    

    public void deleteByCollectionAndEPerson(Context context, Collection collection, EPerson eperson) throws SQLException;

    public List<Subscription> findAllOrderedByEPerson(Context context) throws SQLException;
    
    public void deleteByCommunity(Context context, Community community) throws SQLException;
    public void deleteByCommunityAndEPerson(Context context, Community community, EPerson eperson) throws SQLException;

	public Subscription findByCommunityAndEPerson(Context context, EPerson eperson, Community community) throws SQLException;
}