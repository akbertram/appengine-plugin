
# Jenkins AppEngine Plugin

This plugin can deploy a Java AppEngine application using service account credentials stored with Jenkins, either
as a build step or as workflow step.

Development is at an early stage, with a focus on getting the core use case running, and has several notable gaps:

-  [ ] Integration tests
-  [ ] Support for python and go runtimes
-  [ ] Implement proper auto installer for AppEngine SDKs. Currently the latest release URL is hardcoded
-  [ ] Lookup credentials using the `applicationId` specified in appengine-web.xml if the `applicationId` is not 
       explicitly set
-  [ ] Execution of `set_default_version` directly via the API without the need for a workspace
-  [ ] Plugin wiki page

## Setup 

### SDK Installation

In the "Configure System" page, add an AppEngine SDK Installation.

### Credentials

First, set up Google Service Account credential as described in the 
[https://wiki.jenkins-ci.org/display/JENKINS/Google+OAuth+Plugin](Google OAuth Plugin) documentation for the
project or projects for which you will be deploying AppEngine apps.

If you want the plugin to automatically find the credential, then be sure the project id matches the AppEngine
application id which you are updating

## Usage

### Specifying the path

The plugin expects either a path to the web application directory, or to the base directory of a maven 
application with an exploded war built in the normal place. 

For example, given the following tree:


    <WORKSPACE ROOT>
    |-- appengine-modules-guestbook
    |   |-- src
    |   │-- target
    |   |   |-- appengine-modules-guestbook-1.0
    |   |   |--  WEB-INF
    |   |   |    |-- appengine-web.xml
    |   |   |    |-- web.xml
    
You can provide the plugin either the path:

  * ./appengine-modules-guestbook
  * ./appengine-modules-guestbook/src/target/appengine-modules-guestbook-1.0


## Workflow Step

The workflow step expects to be able to lookup credentials and a default SDK automatically.

You can invoke the step with the same actions available from the 
[https://cloud.google.com/appengine/docs/java/tools/uploadinganapp#Command_Line_Arguments](commandline AppCfg tool).

The following example builds and deploys the appengine-guestbook-example to a non-default version, runs
a simple smoke test on the deployed application before making it the default serving version:

    def appId = "jenkinsplugintest"
    
    node {
        // checkout the repo locally
        git url: "https://github.com/GoogleCloudPlatform/appengine-guestbook-java.git"
    
        // build, run unit tests, and package
        def mvnHome = tool 'Maven 3.2.1'
        sh "${mvnHome}/bin/mvn -B clean install"
    
        // use our build number as the next version
        def newVersion = "build${env.BUILD_NUMBER}"
    
        // deploy to AppEngine, but with a new version that is not the default
        // version but still accessible
        appengine action: "update", applicationId: appId, version: newVersion
    
        def stagingUrl = "https://${newVersion}-dot-${appId}.appspot.com"
    
        echo "Version ${newVersion} staged at ${stagingUrl}"
    
        // Test the new version before making live
        // (poor man's integration test - ideally use web driver!)
        sh "curl ${testUrl} | grep Hello"
    
        appengine action: "set_default_version", applicationId: appId, version: newVersion
    
    }
        
          
      
      
    
    

  

