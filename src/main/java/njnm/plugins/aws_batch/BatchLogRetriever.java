package njnm.plugins.aws_batch;

import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.model.*;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.OutputLogEvent;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by nfultz on 6/11/17.
 */
public class BatchLogRetriever {


    private final int time;

    private final BuildListener listener;
    private final PrintStream logger;
    private final AWSBatch batch;


    private final String jobID, jobName;

    // Pretty printing timestamps, is there a better way?
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public BatchLogRetriever(BuildListener listener, AWSBatch batch, SubmitJobResult sjr, int time) {
        this.listener = listener;
        this.logger = listener.getLogger();
        this.batch = batch;
        this.jobID = sjr.getJobId();
        this.jobName = sjr.getJobName();
        this.time = time;
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private JobDetail jd;
    private JobDetail fetchJobInfo() {
        DescribeJobsResult djr = batch.describeJobs(new DescribeJobsRequest().withJobs(jobID));
        return jd = djr.getJobs().get(0);
    }

    List<AttemptDetail> attempts = new ArrayList<AttemptDetail>();
    private JobStatus status = JobStatus.PENDING;

    private boolean singleLogStep() {
        fetchJobInfo();

        JobStatus newstatus = JobStatus.fromValue(jd.getStatus());
        attempts = jd.getAttempts();


        if(newstatus != status)
            logger.printf("[%s] Attempt %d: %s%n", df.format(new Date()), attempts.size(), status);

        status = newstatus;

        return status == JobStatus.SUCCEEDED  || status == JobStatus.FAILED;

    }


    public boolean doLogging() {
        boolean done = false;
        while(!done){
            done = singleLogStep();

            try {
                TimeUnit.SECONDS.sleep(time);
            } catch (InterruptedException e) {}
        }


        AttemptDetail attempt = attempts.get(attempts.size() - 1);
        Integer exitCode = attempt.getContainer().getExitCode();

        logger.printf("Finished with exit code %d%n", exitCode);

        boolean success = status == JobStatus.SUCCEEDED && exitCode == 0;


        logger.println("Fetching logs from cloudwatch logs for final attempt...");
        logger.println("-------------------------------------------------------");
        String ARN = attempt.getContainer().getTaskArn();

        fetchCloudWatchLogs(ARN);

        return success;

    }


    private void fetchCloudWatchLogs(String ARN) {

        String ecsTaskID = ARN.substring(ARN.indexOf('/') + 1);

        AWSLogs awslogs = AWSLogsClientBuilder.defaultClient();

//        logger.printf("%s %s %s%n%s%n", jobName, jobID, ecsTaskID, ARN);

        GetLogEventsResult logEventsResult =  awslogs.getLogEvents(
                new GetLogEventsRequest()
                        .withLogGroupName("/aws/batch/job")
                        .withLogStreamName(String.format("%s/%s/%s", jobName, jobID, ecsTaskID))
        );

        for(OutputLogEvent ole : logEventsResult.getEvents()) {
            logger.printf("[%s] %s%n", df.format(new Date(ole.getTimestamp())), ole.getMessage());
        }

    }



}
