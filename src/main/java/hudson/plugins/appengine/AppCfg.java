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
import hudson.util.ForkOutputStream;
import net.sf.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public class AppCfg {


    
    public static final String APPCFG_USER = "jenkins";
    
    private final TaskListener listener;
    private final FilePath workspace;

    
    private FilePath appDir;
    private JSONObject credentials;

    private String applicationId;
    private String version;

    private final Launcher launcher;
    private String appCfgPath;
    
    public AppCfg(FilePath workspace, Launcher launcher, TaskListener listener) {
        this.workspace = workspace;
        this.launcher = launcher;
        this.listener = listener;
    }

    public void setAppCfgPath(String path) {
        this.appCfgPath = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getVersion() {
        return version;
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
        throw new AbortException(format("No service account for project '%s'", applicationId));
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

        setPath(projectPath);
    }

    public void setPath(FilePath projectPath) throws IOException, InterruptedException {
        
        if(!projectPath.isDirectory() && projectPath.getName().endsWith(".war")) {
            FilePath tempDir = projectPath.getParent().createTempDir("appengine", "war");
            projectPath.unzip(tempDir);
            
            this.appDir = tempDir;
            
        } else if(projectPath.child("pom.xml").exists()) {
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

        FilePath appEngineXml = getAppEngineWebXmlPath();
        if(!appEngineXml.exists()) {
            throw new AbortException(appEngineXml + " does not exist");
        }
    }

    private FilePath getAppEngineWebXmlPath() {
        return appDir.child("WEB-INF").child("appengine-web.xml");
    }
    

    public void setDefaultVersion() throws IOException, InterruptedException {
        executeOrAbort("set_default_version");
    }
    
    private void parseAppEngineXml() {
        try {
            InputStream in = getAppEngineWebXmlPath().read();

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(in);
            if(Strings.isNullOrEmpty(applicationId)) {
                applicationId = doc.getDocumentElement().getElementsByTagName("application").item(0).getTextContent().trim();
            }
            if(Strings.isNullOrEmpty(version)) {
                version = doc.getDocumentElement().getElementsByTagName("version").item(0).getTextContent().trim();
            }
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse appengine-web.xml", e);
        }
    }
    
    public void update() throws IOException, InterruptedException {
        
        AppCfgResult result = execute("update");
        
        if(result.isConflict()) {
            execute("rollback");
            execute("update");
        }
        
    }
    
    public void executeOrAbort(String action) throws AbortException {
        AppCfgResult result = null;
        try {
            result = execute(action);
        } catch (InterruptedException e) {
            throw new AbortException("Interrupted while waiting for appcfg to complete");
        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
            throw new AbortException(format("Failed to execute %s: %s", action, e.getMessage()));
        }
        if(result.isFailure()) {
            throw new AbortException(format("Failed to execute %s: app.cfg exited with code %d",
                    action, result.getExitCode()));
        }
    }
    
    public AppCfgResult execute(String action) throws IOException, InterruptedException {
        
        if(Strings.isNullOrEmpty(appCfgPath)) {
            throw new AbortException("The path to appcfg.sh has not been set.");
        }
        
        if(credentials == null) {
            setCredentials(findCredentials(applicationId));
        }
        

        // AppCfg locates the stored credentials based on the user's home directory
        // and expects content based on the user's name. To ensure consistency 
        // and avoid interfering with other builds, we'll override these variables
        // to point our workspace and always use the username 'jenkins'
        
        Map<String, String> env = new HashMap<String, String>();
        env.put("_JAVA_OPTIONS", format("-Duser.home=%s -Duser.name=%s",
                workspace.getRemote(), APPCFG_USER));
        
        FilePath credentialsPath = workspace.child(".appcfg_oauth2_tokens_java");
        credentialsPath.write(credentials.toString(), Charsets.UTF_8.name());

        // Build the arguments for this call
        
        ArgumentListBuilder args = new ArgumentListBuilder();
        args.add(appCfgPath);

        if(Strings.isNullOrEmpty(applicationId) || Strings.isNullOrEmpty(version)) {
            parseAppEngineXml();
        }

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


        try {
            ByteArrayOutputStream processOutput = new ByteArrayOutputStream();

            int statusCode = launcher.launch()
                    .cmds(args)
                    .envs(env)
                    .stdout(new ForkOutputStream(processOutput, listener.getLogger()))
                    .join();

            String standardOutput = new String(processOutput.toByteArray(), Charsets.UTF_8);

            return new AppCfgResult(statusCode, standardOutput);
            
        } finally {
            // Don't leave the credentials lying around on disk
            credentialsPath.delete();
        }
    }

}
