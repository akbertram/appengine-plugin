package hudson.plugins.appengine;

import hudson.*;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.*;
import jenkins.model.Jenkins;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;


public class AppCfgInstallation extends ToolInstallation
        implements EnvironmentSpecific<AppCfgInstallation>, NodeSpecific<AppCfgInstallation>, Serializable {

    public static final String UNIX_APP_CFG_COMMAND = "appcfg.sh";
    public static final String WINDOWS_APPCFG_COMMAND = "appcfg.bat";

    private final String sdkHome;

    @DataBoundConstructor
    public AppCfgInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.sdkHome = super.getHome();
    }

    private static String launderHome(String home) {
        if (home.endsWith("/") || home.endsWith("\\")) {
            // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
            // Ant doesn't like the trailing slash, especially on Windows
            return home.substring(0, home.length() - 1);
        } else {
            return home;
        }
    }


    @Override
    public String getHome() {
        if (sdkHome != null) {
            return sdkHome;
        }
        return super.getHome();
    }


    public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            @Override
            public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            }

            public String call() throws IOException {
                File exe = getExeFile();
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    private File getExeFile() {
        String execName = (Functions.isWindows()) ? WINDOWS_APPCFG_COMMAND : UNIX_APP_CFG_COMMAND;
        String sdkHome = Util.replaceMacro(this.sdkHome, EnvVars.masterEnvVars);
        return new File(sdkHome, "bin/" + execName);
    }

    public AppCfgInstallation forEnvironment(EnvVars environment) {
        return new AppCfgInstallation(getName(), environment.expand(sdkHome), getProperties().toList());
    }

    public AppCfgInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new AppCfgInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<AppCfgInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return "AppEngine SDK";
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            String requiredNodeLabel = null;
            return Collections.singletonList(new ZipExtractionInstaller(requiredNodeLabel, 
                    "https://storage.googleapis.com/appengine-sdks/featured/appengine-java-sdk-1.9.17.zip",
                    "appengine-java-sdk-1.9.17"));        
        }

        // for compatibility reasons, the persistence is done by AppCfgBuilder.DescriptorImpl

        @Override
        public AppCfgInstallation[] getInstallations() {
            return Jenkins.getInstance().getDescriptorByType(AppEngineBuildWrapper.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(AppCfgInstallation... installations) {
            Jenkins.getInstance().getDescriptorByType(AppEngineBuildWrapper.DescriptorImpl.class).setInstallations(installations);
        }
    }

    
    public static AppCfgInstallation find(String name) throws AbortException {
        AppCfgInstallation[] installations = ToolInstallation.all()
                .get(AppCfgInstallation.DescriptorImpl.class)
                .getInstallations();
        for(AppCfgInstallation installation : installations) {
            if (name == null || installation.getName().equals(name)) {
                return installation;
            }
        }
        throw new AbortException("There are no AppEngine Java SDKs installed.");
    }
}