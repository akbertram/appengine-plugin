package hudson.plugins.appengine;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Environment;
import hudson.tasks.BuildStepMonitor;

import java.io.IOException;


public abstract class AbstractAppEngineTask extends hudson.tasks.Builder {
    
    
    protected final AppCfg createAppCfg(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws IOException, InterruptedException {
        EnvVars env = abstractBuild.getEnvironment(buildListener);

        AppCfg appCfg = new AppCfg(abstractBuild.getWorkspace(), launcher, buildListener);

        AppEngineBuildWrapper.Environment wrapper = findEnvironment(abstractBuild);
        AppCfgInstallation tool;
        if(wrapper != null) {
            tool = wrapper.getAppCfg();
        } else {
            tool = AppCfgInstallation.find(null);
        }

        String appCfgPath = tool
                .forNode(abstractBuild.getBuiltOn(), buildListener)
                .forEnvironment(env)
                .getExecutable(launcher);

        if (Strings.isNullOrEmpty(appCfgPath)) {
            throw new AbortException("Couldn't obtain path to appcfg.sh from " + tool.getName());
        }

        appCfg.setAppCfgPath(appCfgPath);

        if(wrapper != null) {
            appCfg.setCredentialsId(wrapper.getCredentialsId());
        }

        return appCfg;
    }

    private AppEngineBuildWrapper.Environment findEnvironment(AbstractBuild build) {

        for (Environment environment : build.getEnvironments()) {
            if(environment instanceof AppEngineBuildWrapper.Environment) {
                return (AppEngineBuildWrapper.Environment) environment;
            }
        }
        return null;
    }

    @Override
    public final BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}
