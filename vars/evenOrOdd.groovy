def call(int buildNumber) {
    pipeline {
        stage('Initialize Workspace') {
            echo "The build number is even"
	    deleteDir()
          }
    }
}
