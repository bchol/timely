package timely.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import timely.core.TimesheetEntry;
import timely.service.RecordsService;

@Path("/timeEntry")
public class TimesheetEntryResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimesheetEntryResource.class);

	final RecordsService recSvc;

	public TimesheetEntryResource(RecordsService recSvc) {
		this.recSvc = recSvc;
	}

	//TODO: Move business logic to a service. Keep resource simple
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response findEntries(@Context UriInfo ui) {
		MultivaluedMap<String, String> queryParams = ui.getQueryParameters();

		// Fields to include in output
		String filterInFieldsStr = ui.getQueryParameters().getFirst("showFields");
		// Fields to exclude in output (don't combine with in-fields)
		String filterOutFieldsStr = ui.getQueryParameters().getFirst("removeFields");
		// Date range
		String startDateStr = ui.getQueryParameters().getFirst("start");
		String endDateStr = ui.getQueryParameters().getFirst("end");

		String inFields[] = null;
		String outFields[] = null;

		// Does request explicitly specify which fields to include - for each record?
		if ( filterInFieldsStr != null ) {
			LOGGER.info("findEntries: fields to show: " + filterInFieldsStr);
			inFields = filterInFieldsStr.split(",");
		}
		// Or, does it ask us to remove some fields from each record?
		else if ( filterOutFieldsStr != null ) {
			LOGGER.info("findEntries: fields to remove: " + filterOutFieldsStr);
			outFields = filterOutFieldsStr.split(",");
		}

		// Acceptable query param search fields
		String searchTerms[] = {
				"client",
				"lastName",
				"firstName",
				"department",
				"project",
				"projectCode",
				"isBillable",
				"isInvoiced",
				"isEmployee",
				"isApproved"};

		// Build a collection of records - based on search terms

		// TODO: allow searching numeric-valued fields with comparators

		Set<TimesheetEntry> recSet = null;
		try {
			// Combine search terms by taking successive intersections of
			// each search result set (if specified in query string).
			for ( String param : searchTerms ) {
				String value = queryParams.getFirst(param);
				LOGGER.info("findEntries: request with field " + param + ": " + value);
				// Was this search term specified?
				if ( value != null ) {
					// Find all records with this value for that param (optionally, between theses dates)
					Set<TimesheetEntry> thisParamSet = recSvc.findFieldWithValue(param, value, getDate(startDateStr), getDate(endDateStr));
					if ( thisParamSet != null ) {
						if ( recSet == null ) {
							recSet = thisParamSet;
							LOGGER.info("findEntries: setting current set to those of "+ param 
									+" with value " + value + " (" + thisParamSet.size() + " elements)");
						} else {
							LOGGER.info("findEntries: taking intersection of current set ("
									+recSet.size()+" elements) and set of "+ param 
									+" with value " + value + " (" + thisParamSet.size() + " elements)");

							// take intersection, if needed
							recSet.retainAll(thisParamSet);
							LOGGER.info("findEntries: intersection is of size " + recSet.size());
						}
					}
				}
			}
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | ClassNotFoundException
				| NoSuchMethodException e) {
			LOGGER.warn("findEntries: ERROR: " + e);
			e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		if (recSet == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		} else {
			try {
				if ( inFields != null ) {
					String jsonStr = filterIn(recSet, inFields);
					return Response.status(Response.Status.OK).entity(jsonStr).build();
				} else if ( outFields != null ) {
					String jsonStr = filterOut(recSet, Arrays.asList(outFields));
					return Response.status(Response.Status.OK).entity(jsonStr).build();
				} else {
					return Response.status(Response.Status.OK).entity(recSet).build();
				}
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				LOGGER.info("findEntries: ERROR: " + e);
				return Response.status(Response.Status.BAD_REQUEST).build();
			}
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	//TODO: authenticate
	public Response createEntry(TimesheetEntry entry) {

		//Date,Client,Project,Project Code,
		//Task,Hours,Hours Rounded,
		//isBillable,isInvoiced,isApproved,
		//First Name,Last Name,Department,
		//isEmployee,Billable Rate,Cost Rate,
		//Cost Amount,Currency,External Reference URL

		if ( entry != null ) {
			LOGGER.info("createEntry: POST entry for person: " + entry.getFirstName() + " " + entry.getLastName());

			List<TimesheetEntry> recList = new ArrayList<TimesheetEntry>();
			recList.add(entry);

			recSvc.importRecords(recList);
			return Response.status(Response.Status.CREATED).entity(entry).build();
		} else {
			LOGGER.info("createEntry: invalid entity");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	//TODO: authenticate
	public Response deleteEntry(TimesheetEntry entry) {

		//Date,Client,Project,Project Code,
		//Task,Hours,Hours Rounded,
		//isBillable,isInvoiced,isApproved,
		//First Name,Last Name,Department,
		//isEmployee,Billable Rate,Cost Rate,
		//Cost Amount,Currency,External Reference URL

		if ( entry != null ) {
			LOGGER.info("deleteEntry: DELETE entry for person: " + entry.getFirstName() + " " + entry.getLastName());

			if ( recSvc.deleteEntry(entry) ) {
				return Response.status(Response.Status.NO_CONTENT).build();
			} else {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} else {
			LOGGER.info("deleteEntry: invalid entity");
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
	}

	/**
	 * Removes unwanted fields from a set of TimeData records.
	 * 
	 * @param recSet - set of records to be manipulated and JSONified.
	 * @param fieldsToRemove - list of fields to be removed from each record.
	 * @return a JSON string representing the manipulated set of TimeData records.
	 * @throws JsonProcessingException if the set of TimeData records can't be JSONified.
	 */
	private String filterOut(Set<TimesheetEntry> recSet, List<String>fieldsToRemove) throws JsonProcessingException {
		ObjectMapper objMapper = new ObjectMapper();
		String json = objMapper.writeValueAsString(recSet);

		JSONArray jsonArray = new JSONArray(json);
		JSONArray newArray = new JSONArray();

		// walk through array of JSON structures
		for (int val = 0; val < jsonArray.length(); val++) {
            JSONObject jsonObj = jsonArray.getJSONObject(val);
			LOGGER.info("filterOut: handling field " + val + ": " + jsonObj.toString());

			// remove undesirable fields
			for (String field : fieldsToRemove) {
				jsonObj.remove(field);
			}
            newArray.put(jsonObj);
        }

		return newArray.toString();
	}

	/**
	 * Generates a JSON representation of a set of TimeData records - using
	 * only the fields that are explicitly specified.
	 * 
	 * @param recSet - set of records to be manipulated and JSONified.
	 * @param fieldsToInclude - list of fields to be included from each record.
	 * @return a JSON string representing the manipulated set of TimeData records
	 * @throws JsonProcessingException if the set of TimeData records can't be JSONified
	 */
	private String filterIn(Set<TimesheetEntry> recSet, String fieldsToInclude[]) throws JsonProcessingException {
		ObjectMapper objMapper = new ObjectMapper();
		String json = objMapper.writeValueAsString(recSet);

		JSONArray jsonArray = new JSONArray(json);
		JSONArray newArray = new JSONArray();

		// walk through array of JSON structures
		for (int val = 0; val < jsonArray.length(); val++) {
            JSONObject jsonObj = jsonArray.getJSONObject(val);
            // construct new object based on subset of original
            JSONObject newJsonObj = new JSONObject(jsonObj, fieldsToInclude);
            newArray.put(newJsonObj);
        }

		return newArray.toString();
	}

	private Date getDate(String dateStr) {
	    Date someDate = null;
		if ( dateStr != null ) {
			//TODO: look at joda-time
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
		    try {
			    someDate = sdf.parse(dateStr);
		    } catch (ParseException e) {
		    	LOGGER.info("getDate: invalid date format: " + dateStr);
		    	//TODO: throw. Use an exception mapper, return useful JSON error response.
		    }
		}
	    return someDate;
	}
}
