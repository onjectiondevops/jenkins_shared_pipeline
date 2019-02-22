def call(int buildNumber) {
  if (buildNumber % 2 == 0) {
    pipeline {
        stage('Even Stage') {
            echo "The build number is even"
	    sh "ls -lhtr /"
          }
    }
  } else {
    pipeline {
        stage('Odd Stage') {
            echo "The build number is odd"
	    sh "ls -lhtr /"
      }
    }
  }
}
