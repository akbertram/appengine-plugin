package hudson.plugins.appengine;

import com.google.common.base.Strings;
import hudson.PluginWrapper;
import hudson.model.BuildBadgeAction;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Date;

/**
 * Marks a build as deployed
 */
public class DeploymentBadge implements BuildBadgeAction {

    private String applicationId;
    private String version;
    private Date deploymentDateTime;
    private boolean deployed;

    @DataBoundConstructor
    public DeploymentBadge(String applicationId, String version, Date deploymentDateTime) {
        this.applicationId = applicationId;
        this.version = version;
        this.deploymentDateTime = deploymentDateTime;
    }

    public String getApplicationId() {
        return Strings.nullToEmpty(applicationId);
    }

    public String getVersion() {
        return Strings.nullToEmpty(version);
    }

    @Override
    public String getIconFileName() {
        return getIcon("24x24");
    }

    public Date getDeploymentDateTime() {
        return deploymentDateTime;
    }
    
    public String getIcon(String size) {
        String baseName = "deployed";
        return String.format("%s/plugin/appengine/icons/%s/%s.png", Jenkins.RESOURCE_PATH, size, baseName);
    }

    @Override
    public String getDisplayName() {
        return "Deployed to AppEngine";
    }
    
    public String getDeploymentDescription() {
        return String.format("Deployed %s to %s", version, applicationId);
    }

    @Override
    public String getUrlName() {
        return String.format("https://%s-dot-%s.appspot.com", version, applicationId);
    }

    public boolean overwrites(DeploymentBadge previousBadge) {
        return previousBadge.getApplicationId().equals(getApplicationId()) &&
                previousBadge.getVersion().equals(getVersion());

    }
}
