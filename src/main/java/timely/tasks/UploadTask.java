package timely.tasks;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;

import io.dropwizard.servlets.tasks.PostBodyTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import timely.core.TimesheetEntry;
import timely.service.RecordsService;

public class UploadTask extends PostBodyTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(UploadTask.class);
	private static final int timesheetEntryFieldCount = timely.core.TimesheetEntry.class.getDeclaredFields().length;

	final RecordsService recSvc;

	public UploadTask(RecordsService rs) {
		super("upload");
		this.recSvc = rs;
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, String body, PrintWriter output) throws Exception {

		List<TimesheetEntry> records;
		try {
			records = readFromString( body );
		} catch (InvalidRecordException e) {
			output.print("Trouble importing: " + e.getMessage());
			output.flush();
			return;
		}

		//TODO: use mapDB
		int impCount = recSvc.importRecords(records);

		output.print("Thank you. We imported " + impCount + " of " + (records.size()) + " records.\n");
		output.flush();
	}

	private List<TimesheetEntry> readFromString(String str) {
		ObjectMapper objMapper = new ObjectMapper();
	    Reader inputString = new StringReader(str);
	    BufferedReader inFromUser = new BufferedReader(inputString);

	    Pattern pattern = Pattern.compile(",");

	    // Skip the first line. Split each into fields. Construct and collect records.
		return inFromUser.lines().skip(1).map(line -> {
	        String[] x = pattern.split(line, -1);   // -1 causes it to recognize final blank field
	        int len = x.length;

	        if ( len != timesheetEntryFieldCount ) {
	        	throw new InvalidRecordException("Illegal # of fields: " + len
	        			+ ". Expected " + timesheetEntryFieldCount
	        			+ ". line: \"" + line + "\"\n");
	        }

			return new TimesheetEntry(x[0],         // date
					x[1],                     // client
					x[2],                     // project
					x[3],                     // project code
					x[4],                     // task
					Double.parseDouble(x[5]), // hours
					Double.parseDouble(x[6]), // hoursRounded
					x[7],                     // isBillable
					x[8],                     // isInvoiced
					x[9],                     // isApproved
					x[10],                    // firstName
					x[11],                    // lastName
					x[12],                    // department
					x[13],	    			  // isEmployee
					Integer.parseInt(x[14]),  // billableRate
					Integer.parseInt(x[15]),  // costRate
					Integer.parseInt(x[16]),  // costAmount
					x[17],                    // currency
					x[18]);                   // externalRefUrl
	    }).collect(Collectors.toList());
	}

	private class InvalidRecordException extends RuntimeException {
		public InvalidRecordException(String message) {
			super(message);
		}
		public InvalidRecordException() {}
	}
}
