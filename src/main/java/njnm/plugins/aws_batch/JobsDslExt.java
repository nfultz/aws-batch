package njnm.plugins.aws_batch;

import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslEnvironment;
import javaposse.jobdsl.plugin.DslExtensionMethod;


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
}
