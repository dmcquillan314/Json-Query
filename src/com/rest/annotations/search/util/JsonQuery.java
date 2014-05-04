package com.rest.annotations.search.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonQuery {
	
	private static final String JSON_QUERY_LANGUAGE_REGEX = "^(select)\\s+([a-z0-9_\\,\\.\\s\\*]+)\\s+" +
															"from\\s+([a-zA-Z0-9_\\.]+)\\s*" + 
															"(?:where\\s+\\(?(.+)\\)?)?\\s*" + 
															"(?:order\\sby\\s+([a-z0-9_\\,]+))?\\s*(asc|desc|ascnum|descnum)?\\s*" + 
															"(?:limit\\s+([0-9_\\,]+))?";
	private static final String WHERE_CLAUSE_REGEX = "([\\w\\.]+)\\s*([!=]{2})\\s*\\'([^\\'\\\"]+)\\'\\s*(and|or)?+";
	
	private static Logger log = LoggerFactory.getLogger( JsonQuery.class );
	
	public static List<JSONObject> query( final String sql, final JSONObject json) {
		Pattern pattern = Pattern.compile(JSON_QUERY_LANGUAGE_REGEX, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(sql);
		
		try {
			if ( matcher.matches() ) {
				getLog().info("Running query [ " + matcher.group(1) + " ]");
				
				Map<String,Object> options = new HashMap<String,Object>();
				
				String fields = matcher.group(2);
				if( fields != null && !"".equalsIgnoreCase( fields.trim() ) ) {
					options.put( "fields", fields.replaceAll("\\s*","").split(",") );				
				}
				String from = matcher.group(3);
				if( from != null && !"".equalsIgnoreCase( from.trim() ) ) {
					options.put( "from", from.replaceAll("\\s*","").split("\\.") );				
				}
				String where = matcher.group(4);
				if( where != null && !"".equalsIgnoreCase( where.trim() ) ) {
					options.put( "where", where );				
				}
				String orderBy = matcher.group(5);
				if( orderBy != null && !"".equalsIgnoreCase( orderBy.trim() ) ) {
					options.put( "orderBy", orderBy.replaceAll("\\s*","").split(",") );				
				}
				String order = matcher.group(6);
				if( order != null && !"".equalsIgnoreCase( order.trim() ) ) {
					options.put( "order", order );				
				}
				String limit = matcher.group(7);
				if( limit != null && !"".equalsIgnoreCase( limit.trim() ) ) {
					options.put( "limit", limit.replaceAll("\\s*","").split(",") );				
				}
			    		    
			    return parse(json, options);
			} else {
				getLog().info("Error: invalid syntax");
				return new ArrayList<JSONObject>();
			}
		} catch(Exception e) {
			getLog().error("An error exists within your query or json document", e);
			return new ArrayList<JSONObject>();
		}
	}
	
	private static List<JSONObject> parse(JSONObject json, Map<String, Object> options) {
		Map<String,Object> defaults = new HashMap<String, Object>();
		defaults.put("fields", "*");
		defaults.put("from", new String[] { "json" });
		defaults.put("where", "");
		defaults.put("orderBy", new String[] {} );
		defaults.put("order", "asc");
		defaults.put("limit", new String[] {} );
		
		defaults.putAll(options);
		
	    getLog().info("With Params: " + options.toString() );

	    List<JSONObject> result = new ArrayList<JSONObject>();
	    result = returnFilter(json, defaults);
	    //result = returnOrderBy(result, (String[]) defaults.get("orderBy"), (String) defaults.get("order") );
	    //result = returnLimit(result, (String[]) defaults.get("limit") );
	    
	    return result;
	}

	private static List<JSONObject> returnLimit(List<JSONObject> result,
			String[] limits) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<JSONObject> returnOrderBy(List<JSONObject> result,
			String[] orderBy, String order) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<JSONObject> returnFilter(JSONObject json, Map<String, Object> options) {
		
		String[] from = (String[]) options.get("from");
		JSONArray scope = getScope(json, from);
		List<JSONObject> result = new ArrayList<JSONObject>();
		
		if(options.get("where") == "") {
			options.put("where", "true");
		}
		
		for( int i = 0; i < scope.length(); i++) {
			if( scope.get(i) instanceof JSONObject ) {
				JSONObject jsonObject = scope.getJSONObject(i);
				
				Pattern pattern = Pattern.compile(WHERE_CLAUSE_REGEX);
				Matcher matcher = pattern.matcher((String) options.get("where"));
				Boolean addToResult = true;
				while( matcher.find() ) {
					String compoundKey = matcher.group(1);
					String equalityExpression = matcher.group(2);
					String checkValue = matcher.group(3);
					String compoundJoin = matcher.group(4);
					
					Boolean checkResult = checkExpression(jsonObject, compoundKey.trim(), equalityExpression.trim(), checkValue.trim());
					
					if( checkResult ) {
						addToResult = true;						
					} else {
						if( compoundJoin != null && compoundJoin.equalsIgnoreCase("and") ) {
							addToResult = false;
							break;
						} else if ( compoundJoin != null && compoundJoin.equalsIgnoreCase("or") ) {
							addToResult = false;
						} else {
							addToResult = false;
						}
					}
				}
				
				if(addToResult) {
					result.add( returnFields(jsonObject, (String[]) options.get("fields") ) );
				}
			}
		}

		return result;
	}

	private static JSONObject returnFields(JSONObject jsonObject, String[] fields) {
		
		JSONObject returnObject = new JSONObject();
		
		if(fields == null || fields.length == 0)
			fields = new String[] { "*" };
			
		if(fields[0].equalsIgnoreCase("*"))
			return jsonObject;
		
		for( String field : fields ) {
			if( jsonObject.has(field) ) {
				returnObject.put(field, jsonObject.get(field));
			}
		}
		
		return returnObject;
	}

	private static Boolean checkExpression(final JSONObject jsonObject,
			final String compoundKey, final String equalityExpression, final String checkValue) {
		
		final Object value = resolveCompoundKey( jsonObject, compoundKey); 
		
		if( value == null ) {
			return false;
		}
		
		final String stringValue = value.toString();
		Boolean equalityResult = false;
		
		if( equalityExpression.equals("==") ) {
			equalityResult = true;
		}
		
		if( stringValue.equals( checkValue ) == equalityResult ) {
			return true;
		}
		
		return false;
	}

	private static Object resolveCompoundKey(final JSONObject jsonObject, final String compoundKey ) {
		JSONObject curJSONObject = jsonObject;
		Object toReturn = null;
		final String[] keyParts = compoundKey.split("\\.");
		for( int i = 0; i < keyParts.length; i++ ) {
			final String key = keyParts[i];
			if( curJSONObject instanceof JSONObject && curJSONObject.has(key) ) {
				
				if( i == keyParts.length - 1) {
					toReturn = curJSONObject.get(key);
				} else {
					curJSONObject = curJSONObject.getJSONObject(key);
				}
			}
		}
		
		return toReturn;
	}
	
	private static JSONArray getScope(JSONObject json, String[] from) {
		JSONArray toReturn = new JSONArray();
		JSONObject curObject = json;
		
		for( String str : from ) {
			if( curObject.get(str) instanceof JSONArray ) {
				getLog().debug("A scope of [" + from + "] was requested where one property returned an instance of a JSONArray.  Returning...");
				toReturn = curObject.getJSONArray(str);
			} else if (curObject.get(str) instanceof JSONObject ) {
				curObject = curObject.getJSONObject(str);
			}
		}
		
		return toReturn;
	}

	public static Logger getLog() {
		return log;
	}
}

/*

returnOrderBy: function(result,orderby,order){
	if(orderby.length == 0) 
		return result;
	
	result.sort(function(a,b){	
		switch(order.toLowerCase()){
			case "desc": return (eval('a.'+ orderby[0] +' < b.'+ orderby[0]))? 1:-1;
			case "asc":  return (eval('a.'+ orderby[0] +' > b.'+ orderby[0]))? 1:-1;
			case "descnum": return (eval('a.'+ orderby[0] +' - b.'+ orderby[0]));
			case "ascnum":  return (eval('b.'+ orderby[0] +' - a.'+ orderby[0]));
		}
	});

	return result;	
},

returnLimit: function(result,limit){
	switch(limit.length){
		case 0: return result;
		case 1: return result.splice(0,limit[0]);
		case 2: return result.splice(limit[0]-1,limit[1]);
	}
}
*/