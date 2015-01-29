package hudson.plugins.appengine;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Environment;
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

        AppEngineBuildWrapper.Environment wrapper = findEnvironment(abstractBuild);
        appCfg.setCredentialsId(wrapper.getCredentialsId());
        appCfg.setTool(wrapper.getAppCfg());

        EnvVars env = abstractBuild.getEnvironment(buildListener);
        appCfg.setPath(env.expand(path));
        appCfg.setApplicationId(env.expand(applicationId));
        appCfg.setVersion(env.expand(version));
        appCfg.execute(abstractBuild, "update");
        return true;
    }

    private AppEngineBuildWrapper.Environment findEnvironment(AbstractBuild build) {

        for (Environment environment : build.getEnvironments()) {
            if(environment instanceof AppEngineBuildWrapper.Environment) {
                return (AppEngineBuildWrapper.Environment) environment;
            }
        }
        throw new IllegalStateException("Cannot find AppEngine environment");
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
