package timely.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import timely.core.TimesheetEntry;

//TODO: split into interface/implementation
public class RecordsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(RecordsService.class);

	private final Set<TimesheetEntry> completeSet = new ConcurrentHashMap<TimesheetEntry, Boolean>().newKeySet();

	public int importRecords(List<TimesheetEntry> newRecords) {
		// Remove duplicates
		List<TimesheetEntry> newRecordsCopy = new ArrayList<>(newRecords);
		newRecordsCopy.removeAll(completeSet);

		completeSet.addAll(newRecordsCopy);

		LOGGER.info("importRecords: ADDED " + (newRecordsCopy.size()) + " of "
				+ newRecords.size() + " to collection (any duplicates removed). There are now " + completeSet.size() + " records in total.");
		return newRecordsCopy.size();
	}

	public boolean deleteEntry(TimesheetEntry entryToDelete) {
		LOGGER.info("deleteEntry");

		boolean removed = false;
		for (TimesheetEntry rec : completeSet) {
			if (rec.equals(entryToDelete)) {
				LOGGER.info("deleteEntry: FOUND entry to delete: " + rec.toString() + "\n");
				completeSet.remove(rec); //FIXME: concurrrent?
				removed = true;
			}
		}
		return removed;
	}

	public Set<TimesheetEntry> findFieldWithValue(String field, String valueToFind, Date start, Date end) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
		LOGGER.info("findFieldWithValue: " + valueToFind);

		// Use reflection to get arbitrary TimesheeEntry fields
		// to be used for search terms.
		Method fieldGetter = TimesheetEntry.getGetter(field);

		Set<TimesheetEntry> recList = new HashSet<TimesheetEntry>();
		for (TimesheetEntry rec : completeSet) {
			String recFieldValue = (String) fieldGetter.invoke(rec);
			//TODO: use a configured debug flag
			//LOGGER.info("findFieldWithValue: trying record with " + field + " of " + recFieldValue);
			if (recFieldValue.equalsIgnoreCase(valueToFind)) {
				//LOGGER.info("findFieldWithValue: FOUND record: " + rec.toString() + "\n");
				Date recDate = null;
				if ( start != null || end != null ) {
					SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
					try {
						recDate = sdf.parse(rec.getDate());
					} catch (ParseException e) {
						LOGGER.info("Bad record date format: " + rec.getDate() + ". Skipping this record.");
						continue;
					}
				}
				if ( start != null && recDate.before(start) ) {
					LOGGER.info("Record date " + rec.getDate() + " is before start constraint: " + start + ". Skipping.");
					continue;
				}
				if ( end != null && recDate.after(end) ) {
					LOGGER.info("Record date " + rec.getDate() + " is after end constraint: " + end + ". Skipping.");
					continue;
				}
				recList.add(rec);
			}
		}
		LOGGER.info("findFieldWithValue(" + field + ", " + valueToFind +"): FOUND " + recList.size() + " records.\n");
		return recList;
	}
}
