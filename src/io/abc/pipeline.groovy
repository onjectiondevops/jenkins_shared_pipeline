#!/usr/bin/groovy
package io.abc;

// Cleanup Stages
def Initialize_Workspace(STAGE_NAME) {
  stage(STAGE_NAME){
      wrap([$class: 'BuildUser']) {
      echo "BUILD_USER that started this Pipeline: ${BUILD_USER}"
      }
      deleteDir()
  }
}

// Checkout Functions
def CheckOutScm(STAGE_NAME, REPOSITORY_NAME, BRANCH_NAME, CREDENTIAL_ID){
   stage(STAGE_NAME){
      checkout([$class: 'GitSCM', branches: [[name: BRANCH_NAME]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: CREDENTIAL_ID, url: REPOSITORY_NAME]]])
      gitBranch = BRANCHNAME
   }
}

def CheckOutScmAuto(STAGE_NAME){
   stage(STAGE_NAME){
        gitrepo = checkout scm
        gitBranch = gitrepo.GIT_BRANCH
   }
}

// Code Scanning Stages
def SonarScan(STAGE_NAME, server, projectname, projectkey) {
   stage(STAGE_NAME){
  	  def SonarQubescannerHome = tool 'sonarqube-scanner'
	    sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"
   }
}

//Code Build
def SetEnvironment(STAGE_NAME, DOCKERIMAGENAME, BUILDSTEPSTORUN){
  stage(STAGE_NAME){
      docker.image(DOCKERIMAGENAME).inside {
          sh """ ${BUILDSTEPSTORUN} """
      }
  }
}

// Code Testing
def JmeterPublish(STAGE_NAME, JMETERFILENAME) {
  stage(STAGE_NAME){
    performanceReport parsers: [[$class: 'JMeterParser', glob: JMETERFILENAME]], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5, sourceDataFiles: JMETERFILENAME
  }
}

def MvnStyleBuild(STAGE_NAME, DIRNAME, ARGUMENTS) {
  stage(STAGE_NAME){
     def scannerHome = tool 'mvn'
     sh "cd ${DIRNAME} && ${scannerHome}/bin/mvn ${ARGUMENTS}"
  }
}

def ArtifactArchive(STAGE_NAME, PATHOFARTIFACT){
  stage(STAGE_NAME){
    archiveArtifacts artifacts: PATHOFARTIFACT, fingerprint: true
  }
}

def JunitPublish(STAGE_NAME, PATHOFARTIFACT){
  stage(STAGE_NAME){
     junit PATHOFARTIFACT
  }
}

def SeleniumTest(STAGE_NAME, COMMANDS_TO_RUN){
stage(STAGE_NAME){
  wrap([$class: 'Xvfb']) {
    sh """ ${COMMANDS_TO_RUN} """
    }
 }
}

// Docker Build Stages
def DockerBuild(STAGE_NAME, imagename) {
  stage(STAGE_NAME){
	    sh "docker build -t ${imagename} ."
  }
}

def DockerPush(STAGE_NAME, HUBCREDENTIALID, REGISTRY, IMAGENAME){
  stage(STAGE_NAME){
      withDockerRegistry(credentialsId: HUBCREDENTIALID, url: "${REGISTRY}") {
          sh "docker push ${IMAGENAME}"
      }
  }
}

// Post scan
def TwistLockScan(STAGE_NAME, IMAGENAME){
  stage(STAGE_NAME) {
  twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: true, image: IMAGENAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  }
}

def TwistLockPublish(STAGE_NAME, IMAGENAME){
    stage(STAGE_NAME){
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
def RemoteDockerDeploy(STAGE_NAME, IP, USERNAME, ssh_credentials_id, COMMAND){
   stage(STAGE_NAME){
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

def KubernetesDeployment(STAGE_NAME, CLUSTERNAME, APIENDPOINT, KUBERNETESCREDENTIALID, KUBERNETESCOMMANDS){
  stage(STAGE_NAME){
     withKubeConfig(caCertificate: '', clusterName: "${CLUSTERNAME}", contextName: '', credentialsId: "${KUBERNETESCREDENTIALID}", namespace: '', serverUrl: "${APIENDPOINT}") {
        sh """ ${KUBERNETESCOMMANDS} """
      }
  }
}


return this
