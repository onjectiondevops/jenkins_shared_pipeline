#!/usr/bin/groovy
package io.abc;

def Initialize_Workspace(STAGENAME) {
  stage(STAGENAME){
      wrap([$class: 'BuildUser']) {
      echo "BUILD_USER that started this Pipeline: ${BUILD_USER}"
      }
      deleteDir()
  }
}

def checkOutScm(STAGENAME, REPOSITORYNAME, BRANCHNAME, CREDENTIALID){
   stage(STAGENAME){
      checkout([$class: 'GitSCM', branches: [[name: BRANCHNAME]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: CREDENTIALID, url: REPOSITORYNAME]]])
      gitBranch = BRANCHNAME
   }
}

def checkOutScmAuto(STAGENAME){
   stage(STAGENAME){
        gitrepo = checkout scm
        gitBranch = gitrepo.GIT_BRANCH
   }
}

def MvnBuild(STAGENAME, name) {
   stage(STAGENAME){
      def scannerHome = tool 'mvn'
      sh "${scannerHome}/bin/mvn ${name}"
   }
}

def sonarScan(STAGENAME, server, projectname, projectkey) {
   stage(STAGENAME){
  	  def SonarQubescannerHome = tool 'sonarqube-scanner'
	    sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"
   }
}

def dockerBuild(STAGENAME, imagename) {
  stage(STAGENAME){
	    sh "docker build -t ${imagename} ."
  }
}

def dockerLoginandPush(STAGENAME, HUBCREDENTIALID, REGISTRY, IMAGENAME){
  stage(STAGENAME){
      withDockerRegistry(credentialsId: HUBCREDENTIALID, url: "${REGISTRY}") {
          sh "docker push ${IMAGENAME}"
      }
  }
}

def onlyMasterSteps(stepsToRun) {
      if (gitBranch == 'origin/master') {
            echo "Current branch name is: ${gitBranch}"
            stepsToRun
        }
}

def stopOtherThanMaster() {
      if (gitBranch != 'origin/master') {
            echo "Current branch name is: " + gitBranch
            error "Pipeline is not executing from Master branch. Stopping the pipeline."
            return
        }
}

def remoteDockerDeploy(STAGENAME, IP, USERNAME, ssh_credentials_id, COMMAND){
   stage(STAGENAME){
        withCredentials([string(credentialsId: ssh_credentials_id, variable: 'PASS')]) {
          def remote = [:]
          remote.name = "test"
          remote.host = "${IP}"
          remote.user = "${USERNAME}"
          remote.password = PASS
          remote.allowAnyHosts = true
          sshCommand remote: remote, command: "${COMMAND}"
        }
    }
}

def kubernetesDeployment(STAGENAME, CLUSTERNAME, APIENDPOINT, KUBERNETESCREDENTIALID, KUBERNETESCOMMANDS){
  stage(STAGENAME){
     withKubeConfig(caCertificate: '', clusterName: "${CLUSTERNAME}", contextName: '', credentialsId: "${KUBERNETESCREDENTIALID}", namespace: '', serverUrl: "${APIENDPOINT}") {
        sh 'rm -rf ~/.helm && helm init --client-only'
        sh """ ${KUBERNETESCOMMANDS} """
      }
  }
}

//def blackDuckScan(STAGENAME, BLACKDUCKSEVERURL, USERNAME, ) {
//   stage(STAGENAME){
//      hub-detect --blackduck.hub.url=BLACKDUCKSEVERURL --blackduck.hub.username=username --blackduck.hub.password=******* --blackduck.hub.trust.cert=true
//   }
//}

def setEnvironment(STAGENAME, DOCKERIMAGENAME, BUILDSTEPSTORUN){
  stage(STAGENAME){
      docker.image(DOCKERIMAGENAME).inside {
          sh """ ${BUILDSTEPSTORUN} """
      }
  }
}

def twistLockScan(STAGENAME, IMAGENAME){
  stage(STAGENAME) {
  //twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: true, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: false, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 50
  //twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: false, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: true, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  }
}

def twistLockPublish(STAGENAME, IMAGENAME){
    stage(STAGENAME){
    twistlockPublish ca: '', cert: '', containerized: true, dockerAddress: 'unix:///var/run/docker.sock', image: IMAGENAME, key: '', logLevel: 'true', timeout: 50
    }
}

def jmeterPublish(STAGENAME, JMETERFILENAME) {
  stage(STAGENAME){
    performanceReport parsers: [[$class: 'JMeterParser', glob: JMETERFILENAME]], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5, sourceDataFiles: JMETERFILENAME
  }
}

def mvnStyleBuild(STAGENAME, DIRNAME, ARGUMENTS) {
  stage(STAGENAME){
     def scannerHome = tool 'mvn'
     sh "cd ${DIRNAME} && ${scannerHome}/bin/mvn ${ARGUMENTS}"
  }
}

def artifactArchive(STAGENAME, PATHOFARTIFACT){
  stage(STAGENAME){
    archiveArtifacts artifacts: PATHOFARTIFACT, fingerprint: true
  }
}

def junitPublish(STAGENAME, PATHOFARTIFACT){
  stage(STAGENAME){
     junit PATHOFARTIFACT
  }
}

def seleniumTest(STAGENAME, SELENIUM_NODE_IMAGE, RUN_TIME_ARGUMENTS, COMMANDS_TO_RUN){
  stage(STAGENAME){
      docker.image(SELENIUM_NODE_IMAGE).withRun(RUN_TIME_ARGUMENTS) { c ->
          ${COMMANDS_TO_RUN}
     }
   }
}

return this
