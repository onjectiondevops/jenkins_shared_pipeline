def call() {
      stages {
        stage('Initialize Workspace') {
          steps {
	    deleteDir()
	    sh "ls -lhtr /"
          }
        }
    }
}
