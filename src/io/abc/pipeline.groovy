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
def SonarScan(STAGE_NAME, SERVER, PROJECT_NAME, PROJECT_KEY, CODE_DIR) {
   stage(STAGE_NAME){
    withSonarQubeEnv('sonar_server'){
  	  def SonarQubescannerHome = tool 'sonarqube-scanner'
	    sh "cd ${CODE_DIR} && ${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${SERVER} -Dsonar.projectKey=${PROJECT_NAME} -Dsonar.java.binaries=${SonarQubescannerHome}/bin/ -Dsonar.projectName=${PROJECT_KEY} -Dsonar.sourceEncoding=UTF-8 -Dsonar.webhooks.global.1.url=${SERVER}  -Dsonar.sources=. -Dsonar.projectVersion=1.0"
      sleep 10
   }
  }
}

def SonarQualityGates(STAGE_NAME, QUALITY_GATE_BYPASS){
        stage("Check SonarQube Results"){
            withSonarQubeEnv('sonar_server'){
              	timeout(time: 1, unit: 'HOURS') {
            			def qg = waitForQualityGate()
              			if (qg.status != 'OK') {
                        if (QUALITY_GATE_BYPASS == "true"){
                  				      echo "Bypass SonarQube Quality gate failure: ${qg.status}"
                        } else { error "Pipeline aborted due to quality gate failure: ${qg.status}" }
         		     		} else {
        				echo "Sonar Quality gates passed."
        			        }
                    }
          			}
        }
}

//Code Build
def SetEnvironment(STAGE_NAME, DOCKER_IMAGE_NAME, STEPS_TO_RUN, BYPASS_ARGUMENT){
  stage(STAGE_NAME){
      docker.image(DOCKER_IMAGE_NAME).inside {
          try{
              sh """ ${STEPS_TO_RUN} """
          }
          catch (exc) {
                echo "Failed to test"
                if (BYPASS_ARGUMENT == "true"){
                      echo "Skip failure results"
                }
                else {
                      throw(exc)
                }
          }
      }
  }
}

// Code Testing
def JmeterPublish(STAGE_NAME, JMETER_FILE_NAME) {
  stage(STAGE_NAME){
    performanceReport parsers: [[$class: 'JMeterParser', glob: JMETER_FILE_NAME]], relativeFailedThresholdNegative: 1.2, relativeFailedThresholdPositive: 1.89, relativeUnstableThresholdNegative: 1.8, relativeUnstableThresholdPositive: 1.5, sourceDataFiles: JMETER_FILE_NAME
  }
}

def MvnStyleBuild(STAGE_NAME, DIR_NAME, ARGUMENTS, BYPASS_ARGUMENT) {
  stage(STAGE_NAME){
     def scannerHome = tool 'mvn'
     try {
          sh "cd ${DIR_NAME} && ${scannerHome}/bin/mvn ${ARGUMENTS}"
    }
    catch (exc) {
          echo "Failed to test"
          if (BYPASS_ARGUMENT == "true"){
                echo "Skip failure results"
          }
          else {
                throw(exc)
          }
    }
  }
}

def ArtifactArchive(STAGE_NAME, PATH_OF_ARTIFACT){
  stage(STAGE_NAME){
    archiveArtifacts artifacts: PATH_OF_ARTIFACT, fingerprint: true
  }
}

def JunitPublish(STAGE_NAME, PATH_OF_ARTIFACT){
  stage(STAGE_NAME){
     junit PATH_OF_ARTIFACT
  }
}

def SeleniumTest(STAGE_NAME, COMMANDS_TO_RUN, BYPASS_ARGUMENT){
stage(STAGE_NAME){
  wrap([$class: 'Xvfb']) {
      try{
            sh """ ${COMMANDS_TO_RUN} """
      } catch (exc) {
          if (BYPASS_ARGUMENT == "true"){
                echo "Skip Selenium Test result failure"
          }
          else {
                throw(exc)
          }
      }
  }
 }
}

// Docker Build Stages
def DockerBuild(STAGE_NAME, IMAGE_NAME) {
  stage(STAGE_NAME){
	    sh "docker build -t ${IMAGE_NAME} ."
  }
}

def DockerPush(STAGE_NAME, HUB_CREDENTIAL_ID, REGISTRY, IMAGE_NAME){
  stage(STAGE_NAME){
      withDockerRegistry(credentialsId: HUB_CREDENTIAL_ID, url: "${REGISTRY}") {
          sh "docker push ${IMAGE_NAME}"
      }
  }
}

// Post scan
def TwistLockScan(STAGE_NAME, IMAGE_NAME){
  stage(STAGE_NAME) {
  twistlockScan ca: '', cert: '', compliancePolicy: 'warn', containerized: false, dockerAddress: 'unix:///var/run/docker.sock', gracePeriodDays: 0, ignoreImageBuildTime: true, image: IMAGE_NAME, key: '', logLevel: 'true', policy: 'warn', requirePackageUpdate: false, timeout: 10
  }
}

def TwistLockPublish(STAGE_NAME, IMAGE_NAME){
    stage(STAGE_NAME){
    twistlockPublish ca: '', cert: '', containerized: true, dockerAddress: 'unix:///var/run/docker.sock', image: IMAGE_NAME, key: '', logLevel: 'true', timeout: 50
    }
}

// Conditional Stages
def OnlyMasterSteps(STEPS_TO_RUN) {
      if (gitBranch == 'origin/master') {
            echo "Current branch name is: ${gitBranch}"
            STEPS_TO_RUN
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
def RemoteDockerDeploy(STAGE_NAME, IP, USER_NAME, SSH_CREDENTIALS_ID, COMMAND){
   stage(STAGE_NAME){
        withCredentials([string(credentialsId: SSH_CREDENTIALS_ID, variable: 'PASS')]) {
          def remote = [:]
          remote.name = "test"
          remote.host = "${IP}"
          remote.user = "${USER_NAME}"
          remote.password = PASS
          remote.allowAnyHosts = true
          sshCommand remote: remote, command: "${COMMAND}"
        }
    }
}

def KubernetesDeployment(STAGE_NAME, CLUSTER_NAME, API_END_POINT, KUBERNETES_CREDENTIAL_ID, KUBERNETES_COMMANDS){
  stage(STAGE_NAME){
     withKubeConfig(caCertificate: '', clusterName: "${CLUSTER_NAME}", contextName: '', credentialsId: "${KUBERNETES_CREDENTIAL_ID}", namespace: '', serverUrl: "${API_END_POINT}") {
        sh """ ${KUBERNETESCOMMANDS} """
      }
  }
}

def PipelineLogsArtifact(STAGE_NAME){
    stage(STAGE_NAME){
      sh "cat ${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${BUILD_NUMBER}/log >> pipeline_logs.txt"
      archiveArtifacts artifacts: "pipeline_logs.txt", fingerprint: true
    }
}

return this
