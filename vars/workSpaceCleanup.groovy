def call() {
    pipeline {
      stages {
        stage('Initialize Workspace') {
          steps {
	    deleteDir()
	    sh "ls -lhtr /"
          }
        }
      }
    }
}