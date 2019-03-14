#!/usr/bin/groovy
package io.abc;

def Initialize_Workspace() {
  wrap([$class: 'BuildUser']) {
  echo "BUILD_USER that started this Pipeline: ${BUILD_USER}"
  }
  deleteDir()
}

def checkOutScm(REPOSITORYNAME, BRANCHNAME, CREDENTIALID){
  checkout([$class: 'GitSCM', branches: [[name: BRANCHNAME]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: CREDENTIALID, url: REPOSITORYNAME]]])
  gitBranch = BRANCHNAME
}

def checkOutScmAuto(){
      gitrepo = checkout scm
      gitBranch = gitrepo.GIT_BRANCH
}

def MvnBuild(name) {
  def scannerHome = tool 'mvn'
  sh "${scannerHome}/bin/mvn ${name}"
}

def sonarScan(server, projectname, projectkey) {
  	def SonarQubescannerHome = tool 'sonarqube-scanner'
	  sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"

}

def dockerBuild(imagename) {
	sh "docker build -t ${imagename} ."
}

def dockerLoginandPush(HUBCREDENTIALID, REGISTRY, IMAGENAME){
    withDockerRegistry(credentialsId: HUBCREDENTIALID, url: "${REGISTRY}") {
        sh "docker push ${IMAGENAME}"
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
        error "Pipeline is not executing from Master branch. Stopping the pipeline."
        return
    }
}

def remoteDockerDeploy(IP, USERNAME, ssh_credentials_id, COMMAND){
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

def kubernetesDeployment(CLUSTERNAME, APIENDPOINT, KUBERNETESCREDENTIALID, KUBERNETESCOMMANDS){
 withKubeConfig(caCertificate: '', clusterName: "${CLUSTERNAME}", contextName: '', credentialsId: "${KUBERNETESCREDENTIALID}", namespace: '', serverUrl: "${APIENDPOINT}") {
    sh 'rm -rf ~/.helm && helm init --client-only'
    sh """ ${KUBERNETESCOMMANDS} """
  }
}

//def blackDuckScan(BLACKDUCKSEVERURL, USERNAME, ) {
//hub-detect --blackduck.hub.url=BLACKDUCKSEVERURL --blackduck.hub.username=username --blackduck.hub.password=******* --blackduck.hub.trust.cert=true
//}


return this
