package timely.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import lombok.Data;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;


/**
 * Represents an employee timesheet record.
 * 
 */
@Data
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class TimesheetEntry {
	// NOTE: fields are finalized so lombok includes them in constructor,
	//       and so we can statically count the fields - for simple
	//       CSV import (field-count) validation

	@NotBlank   //trimmed length >0
	final private String date;
	@NotBlank
	final private String client;
	@NotBlank
	final private String project;
	@NotBlank
	final private String projectCode;
	@NotBlank
	final private String task;

	final private double hours;
	final private double hoursRounded;

	final private String isBillable;  //FIXME: annotation for yes/no boolean
	final private String isInvoiced;  //FIXME: annotation for yes/no boolean
	final private String isApproved;  //FIXME: annotation for yes/no boolean

	@NotBlank
	final private String firstName;
	@NotBlank
	final private String lastName;

	//NB: could be blank
	final private String department;

	@NotBlank
	final private String isEmployee;  //FIXME: annotation for yes/no boolean

	@Range(min=0)
	final private int billableRate;
	final private int costRate;
	final private int costAmount;

	@NotBlank
	final private String currency;    //FIXME: annotation for currency?

	//NB: could be blank
	@URL
	final private String externalRefURL;  //FIXME: URL?

	/**
	 * Uses reflection to get arbitrary fields to be used as search terms.
	 * 
	 * @param field name whose getter we seek
	 * @return getter of specified field
	 * @throws ClassNotFoundException if the timeDataClass is coded/specified incorrectly
	 * @throws NoSuchMethodException if the field doesn't exist
	 */
	public static Method getGetter(String field) throws ClassNotFoundException, NoSuchMethodException {
		Method method = null;
		//NB: if we move around TimesheetEntry, this reflection text must change
		Class<?> timeDataClass = Class.forName("timely.core.TimesheetEntry");
		Method[] methods = timeDataClass.getDeclaredMethods();
		for (Method m : methods) {
			String methodName = m.getName();

			// Find the right (getter) method.
			if ( Modifier.isPublic(m.getModifiers())
					&& (m.getParameterTypes().length == 0)
					&& (methodName.equalsIgnoreCase("get" + field) 
						|| methodName.equalsIgnoreCase("is" + field)
					   )
			   ) {
				return m;
			}
		}
		if ( method == null ) {
			throw new NoSuchMethodException("getter for " + field);
		}
		return method;
	}
}
