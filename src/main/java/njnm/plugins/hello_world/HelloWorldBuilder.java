package njnm.plugins.hello_world;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.management.Descriptor;

//import com.amazonaws.services.batch.AWSBatchClientBuilder;


import net.sf.json.JSONObject;

import java.util.HashMap;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #command})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(Build, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class HelloWorldBuilder extends Builder {

    private final  String jobname;
    private final String jobdefinition;
    private final String jobqueue;
    private final  String command;
    private final  String vcpu;
    private final  String memory;
    private final  String retries;
    private final HashMap<String, String> params;
    private final HashMap<String, String> environment;


    /**
     * This annotation tells Hudson to call this constructor, with
     * values from the configuration form page with matching parameter names.
     */
    @DataBoundConstructor
    public HelloWorldBuilder(String jobname, String jobdefinition,
                             String command, String jobqueue, String vcpu,
                             String memory,  String retries,
                             HashMap<String, String> params,
                             HashMap<String, String> environment) {
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

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */

    public String getJobdefinition() {
        return jobdefinition;
    }
    public String getCommand()       { return command;}
    public String getJobname()       { return jobname; }
    public String getJobqueue()      { return jobqueue; }
    public String getVcpu()          { return vcpu; }
    public String getMemory()        { return memory; }
    public String getRetries()       { return retries; }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // this is where you 'build' the projectx
        // since this is a dummy, we just say 'hello world' and call that a build

        // this also shows how you can consult the global configuration of the builder
            listener.getLogger().println(getDescriptor().toString());
            listener.getLogger().println(this.toString());
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

    /**
     * Descriptor for {@link HelloWorldBuilder}.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    // this annotation tells Hudson that this is the implementation of an extension point
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
            return "Say hello world";
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
            awsAccessKey   = json.getString("awsAccessKey");
            awsSecretToken = json.getString("awsSecretToken");
            awsRegion      = json.getString("awsRegion");

            save();
            return true; // indicate that everything is good so far
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
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
