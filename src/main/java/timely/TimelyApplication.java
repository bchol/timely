/**
 * Timely
 * 
 * Demonstration timesheet application that allows:
 *  - importing entries (CSV)
 *  - creating individual entries
 *  - deleting individual entries
 *  - searching for entries with
 *  	- multiple fields
 *  	- start/end dates
 *  - including/excluding fields from output
 */
package timely;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import timely.resources.TimesheetEntryResource;
import timely.service.RecordsService;

public class TimelyApplication extends Application<TimelyConfiguration> {

    public static void main(final String[] args) throws Exception {
        new TimelyApplication().run(args);
    }

    @Override
    public String getName() {
        return "Timely";
    }

    @Override
    public void initialize(final Bootstrap<TimelyConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final TimelyConfiguration configuration,
                    final Environment environment) {

        //TODO: guicify, inject configuration and service(s)

        final RecordsService rs = new RecordsService();

        environment.jersey().register( new TimesheetEntryResource(rs) );

        environment.admin().addTask(new timely.tasks.UploadTask(rs));
        
        //TODO: create healthcheck
    }
}
