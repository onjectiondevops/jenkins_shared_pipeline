@Library('pipeline-library-demo')_
import io.abc.*
x1 = new pipeline()

node{

   x1.Initialize_Workspace()
	 x1.checkOutScm('https://github.com/onjectiondevops/jenkins_shared_pipeline.git', 'master', 'github-cred')
   x1.MvnBuild('clean install')
	 x1.sonarScan('http://af6311723449211e99cc10698fc26a4c-1477373547.us-east-2.elb.amazonaws.com:9000', 'jenkins-release-test-code', 'jenkins-release-test-code')
	 x1.dockerBuild('onjectiondevops/testimage:v1')
	 x1.dockerPush('onjectiondevops/testimage:v1')
   x1.onlyMasterSteps(p5.dockerPush('onjectiondevops/testimage:v1'))
	 x1.stopOtherThanMaster()
	 x1.remoteDockerDeploy('142.93.213.218','root','ssh-credentials','docker ps')
	 x1.kubernetesDeployment('snow-leapord', 'https://my-kubernetes-cluster', '<crednetial-id>','kubectl get pods --all-namespaces')

}
