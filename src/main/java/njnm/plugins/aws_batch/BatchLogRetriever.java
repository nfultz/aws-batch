package njnm.plugins.aws_batch;

import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.model.*;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.GetLogEventsRequest;
import com.amazonaws.services.logs.model.GetLogEventsResult;
import com.amazonaws.services.logs.model.OutputLogEvent;
import hudson.AbortException;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Created by nfultz on 6/11/17.
 */
public class BatchLogRetriever {


    private final int time;

    private final PrintStream logger;
    private final AWSBatch batch;


    private final String jobID;

    // Pretty printing timestamps, is there a better way?
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public BatchLogRetriever(PrintStream logger, AWSBatch batch, String jobID, int time) {
        this.logger = logger;
        this.batch = batch;
        this.jobID = jobID;
        this.time = time;
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static class BatchJobDetail {
        final String jobID;
        final JobStatus jobStatus;
        final int numAttempts;
        private final AttemptContainerDetail _container;

        BatchJobDetail(DescribeJobsResult djr) {
            JobDetail detail = djr.getJobs().get(0);
            jobID = detail.getJobId();
            jobStatus = JobStatus.fromValue(detail.getStatus());
            List<AttemptDetail> attempts = detail.getAttempts();
            numAttempts = attempts.size();
            _container = numAttempts > 0 ? attempts.get(numAttempts - 1).getContainer() : null;
        }

        BatchJobDetail(String jobID){
            this.jobID = jobID;
            jobStatus = null;
            numAttempts = 0;
            _container = null;
        }

        private static final EnumSet<JobStatus> doneStatues = EnumSet.of(JobStatus.SUCCEEDED, JobStatus.FAILED);
        boolean isDone() {
            return doneStatues.contains(jobStatus);
        }

        boolean isSuccess() {
            return numAttempts > 0 && _container.getExitCode() == 0 && jobStatus == JobStatus.SUCCEEDED;
        }


        int getExitCode() {
            return _container.getExitCode();

        }

        String getStreamName() {
            return _container.getLogStreamName();
        }

        @Override
        public String toString() {
            return jobStatus == null ? "Not Started... " : String.format("Attempt %d: %s%n", numAttempts, jobStatus);
        }
    }


    private static BatchJobDetail singleLogStep(BatchJobDetail lastJD, AWSBatch batch, PrintStream logger) {
        DescribeJobsResult djr = batch.describeJobs(new DescribeJobsRequest().withJobs(lastJD.jobID));

        BatchJobDetail jd = new BatchJobDetail(djr);

        if(jd.jobStatus != lastJD.jobStatus)
            logger.printf("[%s] %s", df.format(new Date()), jd);

        return jd;

    }

    private static void doTerminate(BatchJobDetail jd, AWSBatch batch, PrintStream logger) {
        batch.terminateJob(new TerminateJobRequest()
                                .withJobId(jd.jobID)
                                .withReason("Terminated from Jenkins"));
        logger.printf("[%s] Sent Termination Request%n", df.format(new Date()));

    }


    public void doLogging() throws InterruptedException, AbortException {

        BatchJobDetail jd = new BatchJobDetail(jobID);
        boolean isAborted = false;
        while(!jd.isDone()){
            jd = singleLogStep(jd, batch, logger);

            try {
                TimeUnit.SECONDS.sleep(time);
            } catch (InterruptedException e) {
                isAborted = true;
                doTerminate(jd, batch, logger);
            }

        }

        if(isAborted && jd.jobStatus == JobStatus.FAILED) {
//            listener.finished(Result.ABORTED);
            throw new InterruptedException("Killed by Cancel button");
        }

        if(jd.numAttempts == 0){
            logger.println("Failed before any attempts began.");
//            listener.finished(Result.FAILURE);
            throw new AbortException("Didn't send any attempts to AWS");
        }

        logger.printf("Finished with exit code %d%n", jd.getExitCode());

        try {
            fetchCloudWatchLogs(jd.getStreamName(), logger);
        } catch (Exception e){
            logger.printf("[%s] Fetching '%s' failed:%n", df.format(new Date()), jd.getStreamName());
            e.printStackTrace(logger);
        }

        if(!jd.isSuccess()){
//            listener.finished(Result.FAILURE);
            throw new AbortException("Batch ran, but not successful");

        }

//        listener.finished(Result.SUCCESS);


    }


    private void fetchCloudWatchLogs(String logStreamName, PrintStream logger) {
        if(logStreamName == null || "".equals(logStreamName)) return;

        logger.println("Fetching logs from cloudwatch logs for final attempt...");
        logger.println("-------------------------------------------------------");

        AWSLogs awslogs = AWSLogsClientBuilder.defaultClient();

        GetLogEventsResult logEventsResult =  awslogs.getLogEvents(
                new GetLogEventsRequest()
                        .withLogGroupName("/aws/batch/job")
                        .withLogStreamName(logStreamName)
        );

        for(OutputLogEvent ole : logEventsResult.getEvents()) {
            logger.printf("[%s] %s%n", df.format(new Date(ole.getTimestamp())), ole.getMessage());
        }

    }



}
