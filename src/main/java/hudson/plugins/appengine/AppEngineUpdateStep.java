package hudson.plugins.appengine;


import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundSetter;

import javax.inject.Inject;

public class AppEngineUpdateStep extends AbstractStepImpl {

    @DataBoundSetter
    private String sdkName;

    @DataBoundSetter
    private String applicationId;

    @DataBoundSetter
    private String version;

    @DataBoundSetter
    private String path;
    
    @DataBoundSetter
    private String action;

    public AppEngineUpdateStep() {
    }


    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {

        public DescriptorImpl() {
            super(Execution.class);
        }

        @Override
        public String getFunctionName() {
            return "updateAppEngine";
        }

        @Override public String getDisplayName() {
            return "Update an Google AppEngine Application";
        }
    }


    public static final class Execution extends AbstractSynchronousStepExecution<Void> {

        @Inject
        private transient AppEngineUpdateStep step;

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
            appCfg.execute(AppCfg.Action.valueOf(step.action.toUpperCase()));
            return null;
        }
        

        private static final long serialVersionUID = 1L;

    }

}
