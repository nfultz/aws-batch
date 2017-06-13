package njnm.plugins.aws_batch;

import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslEnvironment;
import javaposse.jobdsl.plugin.DslExtensionMethod;

import java.util.List;
import java.util.Map;


@Extension(optional = true)
public class JobsDslExt extends ContextExtensionPoint {

    @DslExtensionMethod(context = StepContext.class)
    public AwsBatchBuilder aws_batch(String jobname, String jobdefinition,
                                     String command, String jobqueue,
                                     int vcpu, int memory, int retries) {
        return new AwsBatchBuilder(jobname, jobdefinition,
                command, jobqueue,
                vcpu, memory, retries);
    }

    @DslExtensionMethod(context = StepContext.class)
    public AwsBatchBuilder aws_batch(String jobname, String jobdefinition,
                                     List<String> command, String jobqueue,
                                     int vcpu, int memory, int retries,
                                     Map<String, String> params, Map<String, String> environment) {
        return new AwsBatchBuilder(jobname, jobdefinition,
                command, jobqueue,
                vcpu, memory, retries,
                params, environment);
    }
}
