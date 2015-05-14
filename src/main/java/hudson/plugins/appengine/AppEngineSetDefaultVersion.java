package hudson.plugins.appengine;

import com.google.common.base.Charsets;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Sets the default version of an application id
 */
public class AppEngineSetDefaultVersion extends AbstractAppEngineTask {

    private String applicationId;
    private String version;

    @DataBoundConstructor
    public AppEngineSetDefaultVersion(String applicationId, String version) {
        this.applicationId = applicationId;
        this.version = version;
    }
    

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) 
            throws InterruptedException, IOException {

        AppCfg appCfg = createAppCfg(build, launcher, listener);
        EnvVars env = build.getEnvironment(listener);

        FilePath tempDir = createFakeWebAppDir(build);
    
        appCfg.setApplicationId(env.expand(applicationId));
        appCfg.setVersion(env.expand(version));
        appCfg.setPath(tempDir);
        appCfg.setDefaultVersion();
        
        return true;
    }

    /**
     * AppCfg will fail if the web app dir is not provided, even if it's not used for the set Default version,
     * so create a fake dir with the minimal appengine-web.xml needed to impress AppCfg.sh
     */
    @SuppressWarnings("SpellCheckingInspection")
    private FilePath createFakeWebAppDir(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        FilePath tempDir = build.getWorkspace().createTempDir("appengine", "staging");
        FilePath webInfDir = tempDir.child("WEB-INF");
        webInfDir.mkdirs();
        
        StringBuilder webXml = new StringBuilder();
        webXml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        webXml.append("<web-app version=\"2.5\" xmlns=\"http://java.sun.com/xml/ns/javaee\">\n");
        webXml.append("</web-app>");
        webInfDir.child("web.xml").write(webXml.toString(), Charsets.UTF_8.name());


        StringBuilder appengineWebXml = new StringBuilder();
        appengineWebXml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        appengineWebXml.append("<appengine-web-app xmlns=\"http://appengine.google.com/ns/1.0\">\n");
        appengineWebXml.append("<application>").append(applicationId).append("</application>\n");
        appengineWebXml.append("<version>").append(version).append("</version>\n");
        appengineWebXml.append("<threadsafe>true</threadsafe>\n");
        appengineWebXml.append("</appengine-web-app>\n");
        webInfDir.child("appengine-web.xml").write(appengineWebXml.toString(), Charsets.UTF_8.name());
        
        return tempDir;
    }


    public String getApplicationId() {
        return applicationId;
    }

    public String getVersion() {
        return version;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Set AppEngine Default Version";
        }
    }

}
