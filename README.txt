# AWS Batch for Jenkins

A plugin which provides a "build step" which triggers a job on AWS Batch via Amazon's Java SDK.
This is still very much WIP.

## Jobs DSL: support
You can easily create AWS Batch jobs using the Jobs DSL, for example:

job('Batch_demo_foo') {
  steps {
        aws_batch('test_batch', 
                        'first-run-job-definition:1', 
                        'echo hello', 
                        'first-run-job-queue', 
                        2, 1000, 1)
    }
}

