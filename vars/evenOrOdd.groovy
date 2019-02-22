def call(int buildNumber) {
  if (buildNumber % 2 == 0) {
    pipeline {
      stages {
        stage('Even Stage') {
          steps {
            echo "The build number is even"
	    sh "ls -lhtr /"
          }
        }
      }
    }
  } else {
    pipeline {
      stages {
        stage('Odd Stage') {
          steps {
            echo "The build number is odd"
	    sh "ls -lhtr /"
          }
        }
      }
    }
  }
}
