package njnm.plugins.aws_batch;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;

import com.amazonaws.services.batch.AWSBatch;
import com.amazonaws.services.batch.AWSBatchClientBuilder;
import com.amazonaws.services.batch.model.ContainerOverrides;
import com.amazonaws.services.batch.model.SubmitJobRequest;


import com.amazonaws.services.batch.model.SubmitJobResult;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
    private final int vcpu;
    private final int memory;
    private final int retries;

    private final HashMap<String, String> params;
    private final HashMap<String, String> environment;


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
        this.vcpu = Integer.parseInt(vcpu);
        this.memory = Integer.parseInt(memory);
        this.retries = Integer.parseInt(retries);
        this.params = null;
        this.environment = null;
    }

    /**
     * We'll use these from the <tt>config.jelly</tt>.
     */

    public String getJobdefinition() { return jobdefinition; }
    public String getJobname()       { return jobname; }
    public String getJobqueue()      { return jobqueue; }

    public List<String> getCommand() { return command;}
    public int getVcpu()             { return vcpu; }
    public int getMemory()           { return memory; }
    public int getRetries()          { return retries; }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // this is where you 'build' the projectx
        // since this is a dummy, we just say 'hello world' and call that a build

        // this also shows how you can consult the global configuration of the builder
            //listener.getLogger().println(getDescriptor().toString());
        listener.getLogger().println(this.toString());

        SubmitJobRequest job = new SubmitJobRequest()
                .withJobName(jobname)
                .withJobDefinition(jobdefinition)
                .withJobQueue(jobqueue)
                .withContainerOverrides(
                        new ContainerOverrides()
                                .withCommand(command)
                                .withMemory(memory)
                                .withVcpus(vcpu)
                );



        DescriptorImpl globals = getDescriptor();
        AWSBatch awsbatch = AWSBatchClientBuilder.standard()
                .withRegion(globals.getAwsRegion())
                .withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(globals.getAwsAccessKey(), globals.getAwsSecretToken())))
                .build();

        SubmitJobResult sjr = awsbatch.submitJob(job);
        listener.getLogger().println("Job Submitted:\n" + sjr.toString());

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
        private String awsAccessKey;
        private String awsSecretToken;
        private String awsRegion;

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
            // System.out.println(json.toString());
            json = json.getJSONObject("awsBatch");
            awsAccessKey   = json.getString("awsAccessKey");
            awsSecretToken = json.getString("awsSecretToken");
            awsRegion      = json.getString("awsRegion");

            save();
            return true; // indicate that everything is good so far
        }


        public String getAwsAccessKey()   { return awsAccessKey; }
        public String getAwsSecretToken() {
            return awsSecretToken;
        }
        public String getAwsRegion()      { return awsRegion ; }

        @Override
        public String toString() {
            return "DescriptorImpl{" +
                    "awsAccessKey='" + awsAccessKey + '\'' +
                    ", awsSecretToken='" + awsSecretToken + '\'' +
                    ", awsRegion='" + awsRegion + '\'' +
                    '}';
        }
    }
}
