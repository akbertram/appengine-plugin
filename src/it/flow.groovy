def appId = "jenkinsplugintest"

node {
    // checkout the repo locally
    git url: "https://github.com/GoogleCloudPlatform/appengine-guestbook-java.git"

    // build, run unit tests, and package
    def mvnHome = tool 'M3'
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
    
      