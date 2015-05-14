package hudson.plugins.appengine;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Date;

public class AppEngineDeploy extends AbstractAppEngineTask {

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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        AppCfg appCfg = createAppCfg(build, launcher, listener);
        EnvVars env = build.getEnvironment(listener);

        appCfg.setPath(env.expand(path));
        appCfg.setApplicationId(env.expand(applicationId));
        appCfg.setVersion(env.expand(version));
        appCfg.update();
        
        updateBadges(appCfg, build);
        
        return true;
    }

    private void updateBadges(AppCfg appCfg, AbstractBuild<?, ?> abstractBuild) throws IOException {
        
        DeploymentBadge lastDeployment = new DeploymentBadge(
                appCfg.getApplicationId(), 
                appCfg.getVersion(), 
                new Date());


        for (AbstractBuild<?, ?> previousBuild : abstractBuild.getProject().getBuilds()) {
            if(!previousBuild.getId().equals(abstractBuild.getId())) {
                boolean dirty = false;
                for (DeploymentBadge previousDeployment : previousBuild.getActions(DeploymentBadge.class)) {
                    if(lastDeployment.overwrites(previousDeployment)) {
                        previousBuild.getActions().remove(previousDeployment);
                        dirty = true;
                    }
                }
                if(dirty) {
                    previousBuild.save();
                }
            }
        }

        abstractBuild.addAction(lastDeployment);
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
