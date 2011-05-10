package jobs.scoring;

import helpers.TimeHelper;

import java.util.Date;
import java.util.List;

import models.Insight;
import models.PeriodEnum;
import models.User;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.On;

/**
 * This Job is used to create the score for each category and to historize these scores
 * 
 * @author jb
 *
 */
// run at 3 in the morning every days
//@On("0 0 3 * * ?")
@Every("1h")
public class BuildInsightValidationAndUserScoreJob extends Job {

	private Date fromDate = null;
	private Date toDate = null;
	
	/**
	 * default constructor : runs the job for the previous day
	 * @param fromDate
	 * @param toDate
	 */
	public BuildInsightValidationAndUserScoreJob() {
		this.fromDate = new DateMidnight().minusDays(1).toDate();
		this.toDate =  new DateMidnight().minusDays(1).toDate();
	}
	
	public BuildInsightValidationAndUserScoreJob(Date fromDate, Date toDate) {
		this.fromDate = fromDate;
		this.toDate = toDate;
	}
	
	public void incrementFromDate() {
		fromDate = new Date(fromDate.getTime() + 24l*60l*60l*1000l);
	}
	
    @Override
    public void doJob() throws Exception {
    	// TEMP
		//if(!TimeHelper.hourAndDayCheck(3, null)) {
		//	return;
		//}
    	
    	Logger.info("BuildInsightValidationAndUserScoreJob begin from %s to %s", fromDate, toDate);
    	
    	if (fromDate.before(new Date(toDate.getTime() + 24l*60l*60l*1000l))) {
	    	Logger.info("********************");
	    	Logger.info("date=" + fromDate);
	    	Logger.info("********************");
	    	
	    	new InsightValidationJob(fromDate, PeriodEnum.THREE_MONTHS, true, this).now();
	    	
    	}
    	
    }
}
