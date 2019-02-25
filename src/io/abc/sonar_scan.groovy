#!/usr/bin/groovy
package io.abc;
def sonar_scan(server, projectname, projectkey) {
  	def SonarQubescannerHome = tool 'sonarqube-scanner'
	sh "${SonarQubescannerHome}/bin/sonar-scanner -Dsonar.host.url=${server} -Dsonar.projectKey=${projectname} -Dsonar.projectName=${projectkey} -Dsonar.sources=. -Dsonar.projectVersion=1.0"

}
return this
