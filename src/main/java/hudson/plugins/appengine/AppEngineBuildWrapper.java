package hudson.plugins.appengine;

import com.google.jenkins.plugins.credentials.domains.RequiresDomain;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Map;


/**
 * Wraps a build, providing the location of the AppEngine SDK and the 
 * credentials needed
 */
@RequiresDomain(DeploymentScopeRequirement.class)
public class AppEngineBuildWrapper extends BuildWrapper {

    private String sdkName;
    private String credentialsId;
    
    public class Environment extends BuildWrapper.Environment {

        public AppCfgInstallation getAppCfg() {
            AppCfgInstallation[] installations = getDescriptor().getInstallations();
            if(installations.length > 0) {
                if (sdkName == null || sdkName.equals("(Default)")) {
                    return installations[0];
                } else {
                    for (AppCfgInstallation i : installations) {
                        if (sdkName != null && i.getName().equals(sdkName)) {
                            return i;
                        }
                    }
                }
            }
            return null;
        }
        
        public String getCredentialsId() {
            return credentialsId;
        }
        
    
    }

    @DataBoundConstructor
    public AppEngineBuildWrapper(String sdkName, String credentialsId) {
        this.sdkName = sdkName;
        this.credentialsId = credentialsId;
    }

    @DataBoundSetter
    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    public String getSdkName() {
        return sdkName;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Setting up Google AppEngine...");
        
        return new Environment();
    }

    @Extension
    public static class DescriptorImpl extends BuildWrapperDescriptor {


        @CopyOnWrite
        private volatile AppCfgInstallation[] installations = new AppCfgInstallation[0];

        public DescriptorImpl() {
            load();
        }

        protected DescriptorImpl(Class<? extends AppEngineBuildWrapper> clazz) {
            super(clazz);
        }

        /**
         * Obtains the {@link AppCfgInstallation.DescriptorImpl} instance.
         */
        public AppCfgInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(AppCfgInstallation.DescriptorImpl.class);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }

        protected void convert(Map<String, Object> oldPropertyBag) {
            if (oldPropertyBag.containsKey("installations")) {
                installations = (AppCfgInstallation[]) oldPropertyBag.get("installations");
            }
        }

        @Override
        public String getHelpFile() {
            return "/plugin/AppCfg/help.html";
        }

        @Override
        public String getDisplayName() {
            return "Configure AppEngine SDK";
        }

        public AppCfgInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(AppCfgInstallation... installations) {
            this.installations = installations;
            save();
        }
    }
} 
