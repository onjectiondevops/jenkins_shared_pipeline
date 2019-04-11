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
      if (gitBranch == 'master') {
            echo "Current branch name is: ${gitBranch}"
            stepsToRun
        }
}

def stopOtherThanMaster() {
      if (gitBranch != 'master') {
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

return this
