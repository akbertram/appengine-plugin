package hudson.plugins.appengine;

import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

@Ignore
public class AppEngineStepTest {

  @Rule
  public JenkinsRule r = new JenkinsRule();

  @Test
  public void configRoundTrip() throws Exception {
    AppEngineStep step1 = new AppEngineStep("update");
    step1.setApplicationId("myAppId");
    step1.setPath("server-module");
    step1.setSdkName("AppEngine SDK 1.2");
    step1.setVersion("1");
    
    AppEngineStep step2 = new StepConfigTester(r).configRoundTrip(step1);
    r.assertEqualDataBoundBeans(step1, step2);
  }

}
