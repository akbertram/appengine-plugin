package hudson.plugins.appengine;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class AppEngineDeploy extends hudson.tasks.Builder {
    
    private String applicationId;
    private String version;
    private String path;

    @DataBoundConstructor
    public AppEngineDeploy(String applicationId, String version, String path) {
        this.applicationId = applicationId;
        this.version = version;
        this.path = path;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> abstractBuild, BuildListener buildListener) {
        return true;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        AppCfg appCfg = new AppCfg(abstractBuild.getWorkspace(), launcher, buildListener);
        appCfg.tryInitFromBuildWrapper(abstractBuild);
        appCfg.setPath(path);
        appCfg.setApplicationId(applicationId);
        appCfg.setVersion(version);
        appCfg.execute(abstractBuild, "update");
        return true;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getVersion() {
        return version;
    }

    public String getPath() {
        return path;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder>  {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy to AppEngine";
        }
    }
}
