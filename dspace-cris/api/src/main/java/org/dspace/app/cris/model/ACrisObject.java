/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import java.beans.PropertyEditor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.cris.model.export.ExportConstants;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.authorize.AuthorizableEntity;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataValueVolatile;
import org.dspace.content.RootObject;
import org.dspace.content.UsageEventEntity;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.discovery.IGlobalSearchResult;
import org.dspace.eperson.EPerson;

import it.cilea.osd.common.core.TimeStampInfo;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.ATypeNestedObject;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;

@MappedSuperclass
public abstract class ACrisObject<P extends Property<TP>, TP extends PropertiesDefinition, NP extends ANestedProperty<NTP>, NTP extends ANestedPropertiesDefinition, ACNO extends ACrisNestedObject<NP, NTP, P, TP>, ATNO extends ATypeNestedObject<NTP>>
		implements AuthorizableEntity, ICrisObject<P, TP>, BrowsableDSpaceObject,
		IExportableDynamicObject<TP, P, ACrisObject<P, TP, NP, NTP, ACNO, ATNO>>, Cloneable, IGlobalSearchResult, UsageEventEntity, RootObject {

	@Transient
	private Map<String, Object> extraInfo = new HashMap<>();
	
 	@Embedded
	private SourceReference sourceReference;

	/** Cris public unique identifier, must be null */
	@Column(nullable = true, unique = true)
	private String crisID;

	private Boolean status;

	@Column(nullable = false, unique = true)
	private String uuid;

	public ACrisObject() {
		this.status = false;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

	/**
	 * Wrapper method
	 * 
	 * @param sourceID
	 */
	public void setSourceID(String sourceID) {
		getSourceReference().setSourceID(sourceID);
	}

	/**
	 * Wrappre method
	 * 
	 * @return
	 */
	public String getSourceID() {
		return getSourceReference().getSourceID();
	}

	public abstract String getPublicPath();

	public abstract String getAuthorityPrefix();

	@Override
	public String getHandle() {
		return uuid.toString();
	}

	@Override
	public UUID getID() {
		return UUID.fromString(getUuid());
	}

	@Override
	public boolean isArchived() {
		return getStatus() != null ? getStatus() : false;
	}

	@Override
	public boolean isWithdrawn() {
		return getStatus() != null ? !getStatus() : false;
	}
	
	public abstract boolean isOwner(EPerson eperson);

	/**
	 * Convenience method to get data from ResearcherPage by a string. For any
	 * existent field name the method must return the relative value (i.e
	 * getMetadata("fullName") is equivalent to getFullName()) but the method
	 * always return a list (with 0, 1 or more elements). For dynamic field it
	 * returns the value of the dynamic field with the shorter name equals to
	 * the argument. Only public values are returned!
	 * 
	 * 
	 * @param dcField
	 *            the field (not null) to retrieve the value
	 * @return a list of 0, 1 or more values
	 */
	public String getMetadata(String field) {
		List<String> result = getMetadataValue(field);
		if (result.isEmpty()) {
			return null;
		}
		return result.get(0);
	}

	public List<String> getMetadataValue(String field) {
		List<String> result = new ArrayList();

		List<P> dyna = getAnagrafica4view().get(field);
		for (P prop : dyna) {
			if (prop.getVisibility() == VisibilityConstants.PUBLIC)
				result.add(prop.toString());
		}

		return result;
	}

    @Override
    public List<IMetadataValue> getMetadata(String schema, String element, String qualifier, String lang) {
        return internalGetMetadata(schema, element, qualifier, false, false);
    }

    public List<IMetadataValue> getMetadata(String schema, String element, String qualifier, String lang, boolean onlyPub) {
        return internalGetMetadata(schema, element, qualifier, false, onlyPub);
    }
    
    private List<IMetadataValue> internalGetMetadata(String schema, String element,
            String qualifier, boolean singleValue, boolean onlyPub)
    {	
		Map<Integer, Object> mapResultsVal = new HashMap<Integer, Object>();
		Map<Integer, String> mapResultsAuth = new HashMap<Integer, String>();
	    String authority = null;
		if ("crisdo".equals(schema) && "name".equals(element)) {
		    String val = getName();
		    if(StringUtils.isBlank(val)) {
		        val = "";
		    }
		    mapResultsVal.put(mapResultsVal.hashCode(), val);
		} else if (!schema.equalsIgnoreCase("cris" + this.getPublicPath())) {
			//perhaps is a nested
			boolean buildMetadata = false;
			if (StringUtils.isNotEmpty(element)) {
				List<ACNO> listNestedObject = ResearcherPageUtils
						.getNestedObjectsByParentIDAndShortname(this.getClassNested(), this.getId(), schema);
				for (ACNO nestedObject : listNestedObject) {
					List<NP> nProprieties = nestedObject.getAnagrafica4view().get(element);

					if (nProprieties != null) {
						for (NP prop : nProprieties) {
							Object val = prop.getObject();
							if (StringUtils.isNotEmpty(qualifier) && val instanceof ACrisObject) {
								authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);								
								qualifier = getCompatibleJDynAShortName((ACrisObject) val, qualifier);
								List pointProps = (List) ((ACrisObject) val).getAnagrafica4view().get(qualifier);
								if (pointProps != null && pointProps.size() > 0) {
									for (Object pprop : pointProps) {
									    mapResultsVal.put(pprop.hashCode(), ((Property) pprop).getObject());
									    mapResultsAuth.put(pprop.hashCode(), authority);
										buildMetadata = true;
									}
								}
							}
							else if (val instanceof ACrisObject) {
								authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);
								mapResultsVal.put(val.hashCode(), ((ACrisObject) val).getName());
								mapResultsAuth.put(val.hashCode(), authority);
								buildMetadata = true;
							} else {
								PropertyEditor propertyEditor = prop.getTypo().getRendering()
										.getPropertyEditor(null);
								propertyEditor.setValue(val);
								mapResultsVal.put(val.hashCode(), propertyEditor.getAsText());
								buildMetadata = true;
							}
						}
					}

				}				
			}
			else {
			    schema = getCompatibleJDynAShortName(this, schema);

	            List<P> proprieties = this.getAnagrafica4view().get(schema);

	            if (proprieties != null && !proprieties.isEmpty()) {
	                for (P prop : proprieties) {
	                    Object val = prop.getObject();
	                    if (StringUtils.isNotEmpty(element) && val instanceof ACrisObject) {
	                        
	                        authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);
	                        mapResultsAuth.put(prop.getId(), authority);
	                        element = getCompatibleJDynAShortName((ACrisObject) val, element);
	                        List pointProps = (List) ((ACrisObject) val).getAnagrafica4view().get(element);
	                        if (pointProps != null && pointProps.size() > 0) {
	                            for (Object pprop : pointProps) {
	                                mapResultsVal.put(pprop.hashCode(), ((Property) pprop).getObject());
	                                mapResultsAuth.put(pprop.hashCode(), authority);
	                                buildMetadata = true;
	                            }
	                        }
	                    } else if (val instanceof ACrisObject) {
	                        authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);
	                        mapResultsVal.put(val.hashCode(), ((ACrisObject) val).getName());
	                        mapResultsAuth.put(val.hashCode(), authority);
	                        buildMetadata = true;
	                    } else {
	                        PropertyEditor propertyEditor = prop.getTypo().getRendering().getPropertyEditor(null);
	                        propertyEditor.setValue(val);
	                        mapResultsVal.put(val.hashCode(), propertyEditor.getAsText());
	                        buildMetadata = true;
	                    }
	                }
	            }
			}
			if(!buildMetadata) {
				return new ArrayList<IMetadataValue>();
			}
		} else {
			element = getCompatibleJDynAShortName(this, element);

			List<P> proprieties = this.getAnagrafica4view().get(element);

			if (proprieties != null && !proprieties.isEmpty()) {
				for (P prop : proprieties) {
					Object val = prop.getObject();
					if (StringUtils.isNotEmpty(qualifier) && val instanceof ACrisObject) {
					    
						authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);
						mapResultsAuth.put(prop.getId(), authority);
						qualifier = getCompatibleJDynAShortName((ACrisObject) val, qualifier);
						List pointProps = (List) ((ACrisObject) val).getAnagrafica4view().get(qualifier);
						if (pointProps != null && pointProps.size() > 0) {
							for (Object pprop : pointProps) {
                                mapResultsVal.put(pprop.hashCode(), ((Property) pprop).getObject());
                                mapResultsAuth.put(pprop.hashCode(), authority);
							}
						}
					} else if (val instanceof ACrisObject) {
						authority = ResearcherPageUtils.getPersistentIdentifier((ACrisObject) val);
                        mapResultsVal.put(val.hashCode(), ((ACrisObject) val).getName());
                        mapResultsAuth.put(val.hashCode(), authority);
					} else {
						PropertyEditor propertyEditor = prop.getTypo().getRendering().getPropertyEditor(null);
						propertyEditor.setValue(val);
						mapResultsVal.put(val.hashCode(), propertyEditor.getAsText());
					}
				}
			}
		}
		List<IMetadataValue> result = new ArrayList<>();
		int idx = 0;
		for (Integer key : mapResultsVal.keySet()) {
			MetadataValueVolatile mresult = new MetadataValueVolatile();
			mresult.schema = schema;
			mresult.element = element;
			mresult.qualifier = qualifier;
			if(mapResultsAuth.containsKey(key)) {
				mresult.setAuthority(mapResultsAuth.get(key));
			} 
			mresult.setConfidence(StringUtils.isNotEmpty(authority) ? Choices.CF_ACCEPTED : Choices.CF_UNSET);
			if(mapResultsVal.containsKey(key)) {
			    mresult.setValue(mapResultsVal.get(key).toString());
			}
			else {
			    mresult.setValue("");
			}
			result.add(mresult);
			idx++;
		}
		return result;
	}

	private String getCompatibleJDynAShortName(ACrisObject aCrisObject, String element) {
		Set<String> keys = aCrisObject.getAnagrafica4view().keySet();
		if (!keys.contains(element)) {
			// DSpace is case insensitive, metadata are all lowercase
			for (String key : keys) {
				if (key.replaceAll("[\\-_]", "").equalsIgnoreCase(element)) {
					return key;
				}
			}
		}
		return element;
	}

	@Override
	public String toString() {
		return getName();
	}

	public String getCrisID() {
		return crisID;
	}

	public void setCrisID(String crisID) {
		this.crisID = crisID;
	}

	abstract public TimeStampInfo getTimeStampInfo();

	public String getNamePublicIDAttribute() {
		return ExportConstants.NAME_PUBLICID_ATTRIBUTE;
	}

	public String getValuePublicIDAttribute() {
		return "" + this.getId();
	}

	public String getNameIDAttribute() {
		return ExportConstants.NAME_ID_ATTRIBUTE;
	}

	public String getValueIDAttribute() {
		if (this.getUuid() == null) {
			return "";
		}
		return "" + this.getUuid().toString();
	}

	public String getNameBusinessIDAttribute() {
		return ExportConstants.NAME_BUSINESSID_ATTRIBUTE;
	}

	public String getValueBusinessIDAttribute() {
		return this.getSourceID();
	}

	public String getNameTypeIDAttribute() {
		return ExportConstants.NAME_TYPE_ATTRIBUTE;
	}

	public String getValueTypeIDAttribute() {
		return "" + getType();
	}

	public String getNameSingleRowElement() {
		return ExportConstants.ELEMENT_SINGLEROW;
	}

	public ACrisObject<P, TP, NP, NTP, ACNO, ATNO> getAnagraficaSupport() {
		return this;
	}

	public abstract Class<ACNO> getClassNested();

	public abstract Class<ATNO> getClassTypeNested();

	public abstract <ACO extends ACrisObject<P, TP, NP, NTP, ACNO, ATNO>> Class<ACO> getCRISTargetClass();
	
	public String getSourceRef() {
		return getSourceReference().getSourceRef();
	}

	public void setSourceRef(String sourceRef) {
		getSourceReference().setSourceRef(sourceRef);
	}

	public SourceReference getSourceReference() {
		if (this.sourceReference == null) {
			this.sourceReference = new SourceReference();
		}
		return sourceReference;
	}

	public void setSourceReference(SourceReference sourceReference) {
		this.sourceReference = sourceReference;
	}

	@Override
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		StringTokenizer dcf = new StringTokenizer(mdString, ".");

		String[] tokens = { "", "", "" };
		int i = 0;
		while (dcf.hasMoreTokens()) {
			tokens[i] = dcf.nextToken().trim();
			i++;
		}
		String schema = tokens[0];
		String element = tokens[1];
		String qualifier = tokens[2];

		List<IMetadataValue> values;
		if ("*".equals(qualifier)) {
			values = getMetadata(schema, element, Item.ANY, Item.ANY);
		} else if ("".equals(qualifier)) {
			values = getMetadata(schema, element, null, Item.ANY);
		} else {
			values = getMetadata(schema, element, qualifier, Item.ANY);
		}

		return values;
	}

	public abstract String getMetadataFieldTitle();

	public ACrisObject<P, TP, NP, NTP, ACNO, ATNO> clone() throws CloneNotSupportedException {
		return (ACrisObject<P, TP, NP, NTP, ACNO, ATNO>) super.clone();
	}
	
    public String getMetadataFieldName(Locale locale) {
        return getMetadataFieldTitle() + locale.getLanguage();
    }

	@Override
	public Map<String, Object> getExtraInfo() {
		if(extraInfo==null) {
			extraInfo = new HashMap<>();
		}
		return extraInfo;
	}
	
	@Override
	public String findHandle(Context context) throws SQLException {
		return getID().toString();
	}

	@Override
	public boolean haveHierarchy() {
		return false;
	}
	
    @Override
    public Integer getLegacyId() {
        return getId();
    }
    
	@Override
	public Date getLastModified() {	
		return getTimeStampInfo().getLastModificationTime();
	}
}
