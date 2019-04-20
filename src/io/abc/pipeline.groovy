#!/usr/bin/groovy
package io.abc;

// Cleanup Stages
def Initialize_Workspace(STAGENAME) {
  stage(STAGENAME){
      wrap([$class: 'BuildUser']) {
      echo "BUILD_USER that started this Pipeline: ${BUILD_USER}"
      }
      deleteDir()
  }
}

// Checkout Functions
def CheckOutScm(STAGENAME, REPOSITORYNAME, BRANCHNAME, CREDENTIALID){
   stage(STAGENAME){
      checkout([$class: 'GitSCM', branches: [[name: BRANCHNAME]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: CREDENTIALID, url: REPOSITORYNAME]]])
      gitBranch = BRANCHNAME
   }
}

def CheckOutScmAuto(STAGENAME){
   stage(STAGENAME){
        gitrepo = checkout scm
        gitBranch = gitrepo.GIT_BRANCH
   }
}

// Code Scanning Stages
def SonarScan(STAGENAME, server, projectname, projectkey) {
   stage(STAGENAME){
  	  def SonarQubescannerHome = tool 'sonarqube-scanner'
	    sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"
   }
}

//Code Build
def SetEnvironment(STAGENAME, DOCKERIMAGENAME, BUILDSTEPSTORUN){
  stage(STAGENAME){
      docker.image(DOCKERIMAGENAME).inside {
          sh """ ${BUILDSTEPSTORUN} """
      }
  }
}

// Code Testing
def JmeterPublish(STAGENAME, JMETERFILENAME) {
  stage(STAGENAME){
    performanceReport parsers: [[$class: 'JMeterParser', glob: JMETERFILENAME]], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5, sourceDataFiles: JMETERFILENAME
  }
}

def MvnStyleBuild(STAGENAME, DIRNAME, ARGUMENTS) {
  stage(STAGENAME){
     def scannerHome = tool 'mvn'
     sh "cd ${DIRNAME} && ${scannerHome}/bin/mvn ${ARGUMENTS}"
  }
}

def ArtifactArchive(STAGENAME, PATHOFARTIFACT){
  stage(STAGENAME){
    archiveArtifacts artifacts: PATHOFARTIFACT, fingerprint: true
  }
}

def JunitPublish(STAGENAME, PATHOFARTIFACT){
  stage(STAGENAME){
     junit PATHOFARTIFACT
  }
}

def SeleniumTest(STAGENAME, COMMANDS_TO_RUN){
stage(STAGENAME){
  wrap([$class: 'Xvfb']) {
    sh """ ${COMMANDS_TO_RUN} """
    }
 }
}

// Docker Build Stages
def DockerBuild(STAGENAME, imagename) {
  stage(STAGENAME){
	    sh "docker build -t ${imagename} ."
  }
}

def DockerPush(STAGENAME, HUBCREDENTIALID, REGISTRY, IMAGENAME){
  stage(STAGENAME){
      withDockerRegistry(credentialsId: HUBCREDENTIALID, url: "${REGISTRY}") {
          sh "docker push ${IMAGENAME}"
      }
  }
}

// Post scan
def TwistLockScan(STAGENAME, IMAGENAME){
  stage(STAGENAME) {
  //twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: true, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: false, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 50
  //twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: false, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: true, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  }
}

def TwistLockPublish(STAGENAME, IMAGENAME){
    stage(STAGENAME){
    twistlockPublish ca: '', cert: '', containerized: true, dockerAddress: 'unix:///var/run/docker.sock', image: IMAGENAME, key: '', logLevel: 'true', timeout: 50
    }
}

// Conditional Stages
def OnlyMasterSteps(stepsToRun) {
      if (gitBranch == 'origin/master') {
            echo "Current branch name is: ${gitBranch}"
            stepsToRun
        }
}

def StopOtherThanMaster() {
      if (gitBranch != 'origin/master') {
            echo "Current branch name is: " + gitBranch
            error "Pipeline is not executing from Master branch. Stopping the pipeline."
            return
        }
}

// Deployment Stages
def RemoteDockerDeploy(STAGENAME, IP, USERNAME, ssh_credentials_id, COMMAND){
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

def KubernetesDeployment(STAGENAME, CLUSTERNAME, APIENDPOINT, KUBERNETESCREDENTIALID, KUBERNETESCOMMANDS){
  stage(STAGENAME){
     withKubeConfig(caCertificate: '', clusterName: "${CLUSTERNAME}", contextName: '', credentialsId: "${KUBERNETESCREDENTIALID}", namespace: '', serverUrl: "${APIENDPOINT}") {
        sh """ ${KUBERNETESCOMMANDS} """
      }
  }
}


return this
