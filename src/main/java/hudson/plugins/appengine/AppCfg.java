package hudson.plugins.appengine;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotCredentials;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.util.ArgumentListBuilder;
import net.sf.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppCfg {


    public enum Action {
        UPDATE
    }
    
    public static final String APPCFG_USER = "jenkins";
    
    private final TaskListener listener;
    private EnvVars env;
    private final FilePath workspace;

    
    private FilePath appDir;
    private JSONObject credentials;

    private String applicationId;
    private String version;

    private final Launcher launcher;
    private String appCfgPath;
    
    public AppCfg(FilePath workspace, Launcher launcher, TaskListener listener, EnvVars env) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.listener = listener;
        this.env = env;
    }

    public void setAppCfgPath(String path) {
        this.appCfgPath = path;
    }

    public AppCfg setSDK(AppCfgInstallation appCfg) throws IOException, InterruptedException {
        AppCfgInstallation tool = appCfg
                .forNode(Computer.currentComputer().getNode(), listener)
                .forEnvironment(env);

        appCfgPath = tool.getExecutable(launcher);
        
        return this;
    }


    public void setVersion(String version) {
        this.version = version;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    
    /**
     * Sets the credentials to be used for this update from a Service Account Private Key.
     * @param id the credential's id
     */
    public void setCredentialsId(String id) throws AbortException {

        GoogleRobotCredentials robotCredentials = GoogleRobotCredentials.getById(id);
        setCredentials(robotCredentials);
    }

    private void setCredentials(GoogleRobotCredentials robotCredentials) throws AbortException {
        Credential credentials;
        try {
            credentials = robotCredentials.getGoogleCredential(new DeploymentScopeRequirement());
            credentials.refreshToken();
            
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException("Failed to obtain credentials for deployment");
        }

        if(credentials.getAccessToken() == null) {
            throw new AbortException("Failed to acquire access token for deployment");
        }

        this.credentials = toJson(credentials);
    }

    private GoogleRobotCredentials findCredentials(String applicationId) throws AbortException {
        List<GoogleRobotCredentials> credentials = CredentialsProvider
                .lookupCredentials(GoogleRobotCredentials.class, (Item) null,
                        ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

        for(GoogleRobotCredentials credential : credentials) {
            if(credential.getProjectId().equals(applicationId)) {
                return credential;
            }
        }
        throw new AbortException(String.format("No service account for project '%s'", applicationId));
    }


    /**
     * Creates a JSON credentials object from a Service Account credentials, which
     * do not have refresh tokens.
     *
     */
    public JSONObject toJson(Credential credential) {
        JSONObject user = new JSONObject();
        user.put("access_token", credential.getAccessToken());
        user.put("expiration_time_millis", credential.getExpirationTimeMilliseconds());
        user.put("refresh_token", "NOT_APPLICABLE");

        JSONObject tokens = new JSONObject();
        tokens.put(APPCFG_USER, user);

        JSONObject root = new JSONObject();
        root.put("credentials", tokens);

        return root;
    }

    public void setPath(String path) throws IOException, InterruptedException {
        
        FilePath projectPath = workspace;
        if(path != null) {
            projectPath = projectPath.child(path);
        }

        if(projectPath.child("pom.xml").exists()) {
            // Deduce the app dir relative to this maven project
            FilePath targetDir = projectPath.child("target");
            FilePath[] warFiles = targetDir.list("*.war");
            if(warFiles == null || warFiles.length == 0) {
                listener.error("Could not locate the war file in " + targetDir);
                throw new AbortException();
            }
            String warFile = warFiles[0].getName();
            this.appDir = targetDir.child(warFile.substring(0, warFile.length() - ".war".length()));

        } else {
            this.appDir = projectPath;
        }

        if(!appDir.exists() || !appDir.isDirectory()) {
            throw new AbortException("Exploded war directory does not exist at " + appDir);
        }

        FilePath appEngineXml = appDir.child("WEB-INF").child("appengine-web.xml");
        if(!appEngineXml.exists()) {
            throw new AbortException(appEngineXml + " does not exist");
        }
    }
    
    public int execute(Action action) throws IOException, InterruptedException {
        return execute(action.name().toLowerCase());
    }

    public int execute(String action) throws IOException, InterruptedException {

        if(Strings.isNullOrEmpty(appCfgPath)) {
            setSDK(AppCfgInstallation.find(null));
        }

        if(Strings.isNullOrEmpty(appCfgPath)) {
            throw new AbortException("AppEngine SDK path is not set and no installations are available.");
        }
        
        if(credentials == null) {
            setCredentials(findCredentials(applicationId));
        }
        


        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(appCfgPath);
        
        args.add("--disable_update_check");

        // Authenticate using OAuth2
        args.add("--oauth2");

        if(applicationId != null) {
            args.addKeyValuePair("--", "application",  applicationId, false);
        }
        if(version != null) {
            args.addKeyValuePair("--", "version", version, false);
        }
        
        args.add(action);
        args.add(appDir);


        // AppCfg locates the stored credentials based on the user's home directory
        // and expects content based on the user's name. To ensure consistency 
        // and avoid interfering with other builds, we'll override these variables
        // to point our workspace and always use the username 'jenkins'
        
        Map<String, String> env = new HashMap<String, String>();
        env.put("_JAVA_OPTIONS", String.format("-Duser.home=%s -Duser.name=%s", 
                workspace.getRemote(), APPCFG_USER));
        
        FilePath credentialsPath = workspace.child(".appcfg_oauth2_tokens_java");
        credentialsPath.write(credentials.toString(), Charsets.UTF_8.name());        
        try {
            return launcher.launch().cmds(args).envs(env).stdout(listener).join();
            
        } finally {
            // Don't leave the credentials lying around on disk
            credentialsPath.delete();
        }
    }

    public void execute(AbstractBuild build, Action action) throws IOException, InterruptedException {
        int resultCode = execute(action);
        boolean success = (resultCode == 0);
        // if the build is successful then set it as success otherwise as a failure.
        build.setResult(Result.SUCCESS);
        if (!success) {
            build.setResult(Result.FAILURE);
        }
    }

}
