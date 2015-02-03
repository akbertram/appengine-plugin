package hudson.plugins.appengine;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.jenkins.plugins.credentials.oauth.GoogleRobotPrivateKeyCredentials;
import com.google.jenkins.plugins.credentials.oauth.JsonServiceAccountConfig;
import com.google.jenkins.plugins.credentials.oauth.ServiceAccountConfig;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.tasks.Maven;
import hudson.tools.ToolProperty;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class AppEngineStepIT {

  @Rule
  public JenkinsRule rule = new JenkinsRule();
  
  
  @Before
  public void setUp() throws Exception {
    rule.jenkins.getInjector().injectMembers(this);

    // Setup Maven 3
    Maven.MavenInstallation m3 = new Maven.MavenInstallation("M3", getEnvVariableOrDie("M2_HOME"), null);
    rule.jenkins.getDescriptorByType(Maven.DescriptorImpl.class).setInstallations(m3);
    
    // Setup the AppEngine SDK for this instance
    AppCfgInstallation tool = new AppCfgInstallation("SDK", getEnvVariableOrDie("APPENGINE_SDK_HOME"), Collections.<ToolProperty<?>>emptyList());
    AppCfgInstallation.DescriptorImpl descriptor = rule.jenkins.getDescriptorByType(AppCfgInstallation.DescriptorImpl.class);
    descriptor.setInstallations(tool);
    
    // Setup the Service Account Key
    SystemCredentialsProvider.getInstance().getCredentials().add(
            new GoogleRobotPrivateKeyCredentials("jenkinsplugintest", loadJsonKey(), null));
  }

          
  private String getEnvVariableOrDie(String name) {
    String path = System.getenv(name);
    if(Strings.isNullOrEmpty(path)) {
      throw new RuntimeException(String.format("The environment variable '%s' must be set", name));
    }
    return path;
  }
  
  
  private ServiceAccountConfig loadJsonKey() throws IOException {
    return new JsonServiceAccountConfig(new KeyFileItem(), null);
  }

  @Test
  public void workflowStep() throws Exception {
    
    WorkflowJob job = rule.jenkins.createProject(WorkflowJob.class, "workflow");
    job.setDefinition(new CpsFlowDefinition(readScript("flow.groovy")));
    WorkflowRun run = job.scheduleBuild2(0).get();
    rule.assertBuildStatusSuccess(run);
    
    System.out.println(JenkinsRule.getLog(run));
  }
  
  @Test
  public void freeStyleStep() throws Exception {
    FreeStyleProject job = rule.createFreeStyleProject("freestyle");
    job.setScm(new GitSCM("https://github.com/akbertram/appengine-guestbook-java.git"));
    job.getBuildersList().add(new Maven("clean install", "M3"));
    job.getBuildersList().add(new AppEngineDeploy("jenkinsplugintest", "freestyle", "."));
    FreeStyleBuild run = job.scheduleBuild2(0).get();
    rule.assertBuildStatusSuccess(run);

    System.out.println(JenkinsRule.getLog(run));

  }

  private String readScript(String script) throws IOException {
    URL resource = Resources.getResource(AppEngineStepIT.class, script);
    return Resources.toString(resource, Charsets.UTF_8);
  }
}
