@Library('pipeline-library-demo')_
import io.abc.*
x1 = new pipeline()

node{

   x1.Initialize_Workspace()
	 x1.checkOutScm('https://github.com/onjectiondevops/jenkins_shared_pipeline.git', 'master', 'github-cred')
   x1.MvnBuild('clean install')
	 x1.sonarScan('http://35.200.171.150:9000', 'myproject', 'myproject')
	 x1.dockerBuild('onjectiondevops/testimage:v1')
	 x1.dockerPush('onjectiondevops/testimage:v1')
   x1.onlyMasterSteps(p5.dockerPush('onjectiondevops/testimage:v1'))
	 x1.stopOtherThanMaster()
	 x1.remoteDockerDeploy('142.93.213.218','root','ssh-credentials','docker ps')
	 x1.kubernetesDeployment('show-leapord', 'https://my-kubernetes-cluster', '<crednetial-id>','kubectl get pods --all-namespaces')

}
