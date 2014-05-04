package com.rest.annotations.search.service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.javatuples.Quartet;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rest.annotations.search.exception.AnnotationSearchServiceException;
import com.rest.annotations.search.service.restriction.Restriction;
import com.rest.annotations.search.util.JsonQuery;

import restDocGen.io.file.util.RestDocImportExportUtil;

public class JsonQueryAnnotationSearchService implements AnnotationSearchService {

	private static Logger log = LoggerFactory.getLogger(JsonQueryAnnotationSearchService.class);
	
	private static final String SELECT_TEMPLATE = "select {0} from {1} ";
	private static final String WHERE = "where ({0}) ";
	private static final String WHERE_CLAUSE_TEMPLATE = " {0} {1}.{2} {3} ''{4}'' ";
	private static final String ORDER_BY_TEMPLATE = "order by {0} {1}";
	private static final String LIMIT_TEMPLATE = "limit {0},{1}";
	private static final String REST_API_KEY = "restApi";
	private static final String OR = "or";
	private static final String AND = "and";
	
	private String exportLocation = null;
	private String query = "";
	private String whereClause = "";
	
	public JSONObject executeQuery() throws AnnotationSearchServiceException {
		
		concatQuery(getWhereString());
		
		if( exportLocation == null ) {
			getLog().error("An export location must be set using setExportLocation");
			throw new AnnotationSearchServiceException("An export location must be set using setExportLocation");
		}
		
		JSONObject toReturn = new JSONObject();

		try {
			final Map<String, Object> restApiMap = RestDocImportExportUtil.importRestApiFromJSON(exportLocation);
			final JSONObject jsonObject = new JSONObject(restApiMap);
									
			final List<JSONObject> results = JsonQuery.query(getQuery(), jsonObject);
			
			toReturn.put("results", results);
			return toReturn;
			
		} catch (IOException ioe) {
			throw new AnnotationSearchServiceException("An export location must be set using setExportLocation", ioe);
		} catch (JSONException je) {
			throw new AnnotationSearchServiceException("An export location must be set using setExportLocation", je);
		}
	}

	private String getWhereString() {
		
		final String whereClauseString = getWhereClause();
		return MessageFormat.format(WHERE, new Object[] { whereClauseString });
	}

	private String getSelectString(final String[] fields) {
		
		String fieldArgs = null;
		if( fields == null || fields.length == 0) {
			fieldArgs = "*";
		} else {
			fieldArgs = joinStringArray(fields, ",");
		}
		
		return MessageFormat.format(SELECT_TEMPLATE, new Object[] { fieldArgs, REST_API_KEY });
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String joinStringArray(final String[] parts, final String separator) {
		Iterator<String> iter = (new ArrayList(Arrays.asList(parts))).iterator();
		StringBuilder sb = new StringBuilder();
		if (iter.hasNext()) {
		  sb.append(iter.next());
		  while (iter.hasNext()) {
		    sb.append(separator).append(iter.next());
		  }
		}
		return sb.toString();
	}
	
	public AnnotationSearchService select() {
		return select(new String[] { "*" });
	}
	
	public AnnotationSearchService select(String[] fields) {
		
		concatQuery(getSelectString(fields));
		
		return this;
	}

	public AnnotationSearchService where(final Restriction restriction) {

		concatWhereClause( getWhereClauseString("", restriction) );
		
		return this;
	}

	public AnnotationSearchService and(Restriction restriction) {
		
		concatWhereClause( getWhereClauseString(AND, restriction) );
		
		return this;
	}

	public AnnotationSearchService or(Restriction restriction) {
		
		concatWhereClause( getWhereClauseString(OR, restriction) );
		
		return this;
	}

	public AnnotationSearchService orderBy(String[] property, String order) {
		
		return this;
	}

	public AnnotationSearchService limit(String[] limits) {
		
		return this;
	}

	public String getWhereClauseString( final String joinOperation, final Restriction restriction ) {
		StringBuilder stringBuilder = new StringBuilder();
		final String simpleClassName = restriction.getAnnotation().getSimpleName();
		stringBuilder.append( MessageFormat.format(WHERE_CLAUSE_TEMPLATE, new Object[] { 
																joinOperation,
																simpleClassName, 
																restriction.getProperty(), 
																restriction.getCheckType().toString(),
																restriction.getCheckValue()
															}) );
		return stringBuilder.toString();
	}
	
	public void concatQuery(final String queryPart) {
		this.setQuery(concatString(this.getQuery(), queryPart));
	}
	
	public void concatWhereClause(final String whereClausePart) {
		this.setWhereClause(concatString(getWhereClause(), whereClausePart));
	}
	
	public String concatString(String base, final String append) {
		StringBuilder sb = new StringBuilder(base);
		sb.append(append);
		return sb.toString();
	}
	
	public String getExportLocation() {
		return exportLocation;
	}

	public void setExportLocation(String exportLocation) {
		this.exportLocation = exportLocation;
	}

	public static Logger getLog() {
		return log;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getWhereClause() {
		return whereClause;
	}

	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

}
