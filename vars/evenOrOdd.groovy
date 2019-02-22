def call(int buildNumber) {
    pipeline {
      agent any
      stages {
        stage('Initialize Workspace') {
          steps {
	    deleteDir()
          }
        }
      }
    }
}
