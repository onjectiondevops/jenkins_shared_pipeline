def call() {
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
