package njnm.plugins.aws_batch;

import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.*;


import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.management.Descriptor;


import net.sf.json.JSONObject;

import java.util.*;

/**
 * AWS Batch Builder {@link Builder}.
 *
 * <p>
 *     This provides a minimum viable product for running jobs on AWS batch from jenkins.
 *
 * @author Neal Fultz
 */


public class AwsBatchBuilder extends Builder {

    // Job fields
    private final String jobname;
    private final String jobdefinition;
    private final String jobqueue;

    // Container overrides
    private final List<String> command;
    private final Integer vcpu;
    private final Integer memory;
    private final Integer retries;

    private final Map<String, String> params;
    private final Map<String, String> environment;


    /**
     * This annotation tells Hudson to call this constructor, with
     * values from the configuration form page with matching parameter names.
     */
    @DataBoundConstructor
    public AwsBatchBuilder(String jobname, String jobdefinition,
                           String command, String jobqueue,
                           String vcpu, String memory, String retries){/*,
                             HashMap<String, String> params,
                             HashMap<String, String> environment) {*/
        this.jobname = jobname;
        this.jobdefinition = jobdefinition;
        this.jobqueue = jobqueue;
        this.command = Arrays.asList(command.split("\\s+"));
        this.vcpu = parseIntOrNull(vcpu);
        this.memory = parseIntOrNull(memory);
        this.retries = parseIntOrNull(retries);
        this.params = null;
        this.environment = null;
    }

    public AwsBatchBuilder(String jobname, String jobdefinition,
                           String command, String jobqueue,
                           Integer vcpu, Integer memory, Integer retries) {
        this.jobname = jobname;
        this.jobdefinition = jobdefinition;
        this.jobqueue = jobqueue;
        this.command = Arrays.asList(command.split("\\s+"));
        this.vcpu = vcpu;
        this.memory = memory;
        this.retries = retries;
        this.params = null;
        this.environment = null;

    }

    public AwsBatchBuilder(String jobname, String jobdefinition,
                           List<String> command, String jobqueue,
                           Integer vcpu, Integer memory, Integer retries,
                           Map<String,String> params, Map<String,String> environment) {
        this.jobname = jobname;
        this.jobdefinition = jobdefinition;
        this.jobqueue = jobqueue;
        this.command = command;
        this.vcpu = vcpu;
        this.memory = memory;
        this.retries = retries;
        this.params = params;
        this.environment = environment;

    }

    private static Integer parseIntOrNull(String strVal){
        if(strVal == null || strVal.isEmpty()) return null;

        try {
            return Integer.parseInt(strVal);
        }
        catch (java.lang.NumberFormatException nfe) {
            throw new IllegalArgumentException("Can't parse "+ strVal + " to an integer");
        }
    }

    private static String intOrNullToString(Integer i) {
        return i == null ? "" : i.toString();
    }

    /**
     * We'll use these from the <tt>config.jelly</tt>.
     */

    public String getJobdefinition() { return jobdefinition; }
    public String getJobname()       { return jobname; }
    public String getJobqueue()      { return jobqueue; }

    public String getCommand() {
        StringBuilder ret = new StringBuilder();
        for(String s: command) {
            ret.append(s);
            ret.append(" ");
        }
        return ret.toString().trim();
    }

    public String getVcpu()             { return intOrNullToString(vcpu); }
    public String getMemory()           { return intOrNullToString(memory); }
    public String getRetries()          { return intOrNullToString(retries); }

    private ContainerOverrides getContainerOverrides() {
        ContainerOverrides containerOverrides = new ContainerOverrides();
        if(!command.get(0).contentEquals("")) containerOverrides.setCommand(command);
        if(memory != null)  containerOverrides.setMemory(memory);
        if(vcpu != null)    containerOverrides.setVcpus(vcpu);
        if(environment != null) containerOverrides.setEnvironment(mapToColl(environment));

        return containerOverrides;
    }

    private static Collection<KeyValuePair> mapToColl(Map<String, String> map) {
        List<KeyValuePair> ret = new ArrayList<>();
        for(Map.Entry<String, String> e : map.entrySet())
            ret.add(new KeyValuePair().withName(e.getKey()).withValue(e.getValue()));

        return ret;

    }

    private SubmitJobRequest getSubmitJobRequest() {
        SubmitJobRequest job = new SubmitJobRequest()
                .withJobName(jobname)
                .withJobDefinition(jobdefinition)
                .withJobQueue(jobqueue)
                .withContainerOverrides(
                        getContainerOverrides()
                );

        if(retries != null) job.setRetryStrategy(new RetryStrategy().withAttempts(retries));
        if(params != null) job.setParameters(params);

        return job;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws AbortException {

        SubmitJobRequest job = getSubmitJobRequest();

        listener.getLogger().println(job.toString());


        AWSBatch awsbatch = AWSBatchClientBuilder.defaultClient();

        SubmitJobResult sjr = awsbatch.submitJob(job);
        listener.getLogger().println("Job Submitted:%n" + sjr.toString());

        BatchLogRetriever retriever = new BatchLogRetriever(listener, awsbatch, sjr, getDescriptor().logPollingFreq);

        boolean success = retriever.doLogging();

        if(!success) throw new AbortException(); // Docs say returning false is deprecated, and to throw an exception instead

        return true;
    }



    /**
     * Hudson defines a method {@link Builder#getDescriptor()}, which
     * returns the corresponding {@link Descriptor} object.
     *
     * Since we know that it's actually {@link DescriptorImpl}, override
     * the method and give a better return type, so that we can access
     * {@link DescriptorImpl} methods more easily.
     *
     * This is not necessary, but just a coding style preference.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl)super.getDescriptor();
    }

    @Override
    public String toString() {
        return "AwsBatchBuilder{" +
                "jobname='" + jobname + '\'' +
                ", jobdefinition='" + jobdefinition + '\'' +
                ", jobqueue='" + jobqueue + '\'' +
                ", command='" + command + '\'' +
                ", vcpu=" + vcpu +
                ", memory=" + memory +
                ", retries=" + retries +
                '}';
    }

    /**
     * Descriptor for {@link AwsBatchBuilder}.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

        private int logPollingFreq = 15;

        public DescriptorImpl() {
            load();
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Run a job on AWS Batch";
        }

        /**
         * Applicable to any kind of project.
         */
        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().
            //System.out.println(json.toString());
            if(json.containsKey("logPollingFreq")) {
                logPollingFreq = json.getInt("logPollingFreq");
            }
            else {
                logPollingFreq = 15;
            }
            save();
            return true; // indicate that everything is good so far
        }

        public int getLogPollingFreq() {
            return logPollingFreq;
        }
    }

}
