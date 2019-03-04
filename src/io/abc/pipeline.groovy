#!/usr/bin/groovy
package io.abc;

def Initialize_Workspace() {
  deleteDir()
}

def build(name) {
  def scannerHome = tool 'mvn'
  sh "${scannerHome}/bin/mvn ${name}"
}

def sonarScan(server, projectname, projectkey) {
  	def SonarQubescannerHome = tool 'sonarqube-scanner'
	sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"

}

def dockerBuild(name) {
	app = docker.build("${name}")
}

def onlymaster() {
  if (env.BRANCH_NAME != 'master') {
        echo "skipping onlyOnMaster steps for branch '${env.BRANCH_NAME}'"
        return
    }
}

return this
