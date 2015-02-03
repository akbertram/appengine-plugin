package hudson.plugins.appengine;


import hudson.*;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.inject.Inject;

public class AppEngineStep extends AbstractStepImpl {

    private final String action;
    private String sdkName;
    private String applicationId;
    private String version;
    private String path;
    

    @DataBoundConstructor
    public AppEngineStep(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public String getSdkName() {
        return sdkName;
    }

    @DataBoundSetter
    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    @DataBoundSetter
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getVersion() {
        return version;
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    @DataBoundSetter
    public void setPath(String path) {
        this.path = path;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "appengine";
        }

        @Override public String getDisplayName() {
            return "Update an Google AppEngine Application";
        }
    }


    public static final class Execution extends AbstractSynchronousStepExecution<Void> {

        @Inject
        private transient AppEngineStep step;

        @StepContextParameter
        private transient FilePath workspace;

        @StepContextParameter
        private transient EnvVars env;

        @StepContextParameter
        private transient Launcher launcher;

        @StepContextParameter
        private transient TaskListener taskListener;

        @StepContextParameter
        private transient Node node;
        

        @Override
        protected Void run() throws Exception {
            AppCfg appCfg = new AppCfg(workspace, launcher, taskListener, env);
            appCfg.setSDK(AppCfgInstallation.find(step.sdkName));
            appCfg.setApplicationId(step.applicationId);
            appCfg.setVersion(step.version);
            appCfg.setPath(step.path);
            int exitCode = appCfg.execute(step.action);
            if(exitCode != 0) {
                throw new AbortException(String.format("AppEngine %s failed with exit code %d", step.action, exitCode));
            }
            return null;
        }

        private static final long serialVersionUID = 1L;

    }

}
