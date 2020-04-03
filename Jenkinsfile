pipeline {
  agent any
  options {
    timestamps()
    timeout(time: 4, unit: 'HOURS')
  }
  parameters {
    string(name: 'CREATE_RELEASE', defaultValue: 'false')
    string(name: 'VERSION', defaultValue: '8.0.1.artifactbinding-1.1')
    string(name: 'REPO_URL', defaultValue: '')
    string(name: 'SKIP_TESTS', defaultValue: 'true')
  }
  environment{
    APP="keycloak"
  }
  stages {
    stage('Build') {
      agent {
        label 'jenkins-slave-maven-ct'
      }
      steps {
        script {
          sh 'printenv'
          sh """
            mvn -B -T4 clean install -DskipTests=${params.SKIP_TESTS} -Pdistribution
          """
          if (params.CREATE_RELEASE == "true"){
            echo "creating release ${VERSION} and uploading it to ${REPO_URL}"
            // upload to repo
            withCredentials([usernamePassword(credentialsId: 'cloudtrust-cicd-artifactory-opaque', usernameVariable: 'USR', passwordVariable: 'PWD')]){
              sh """
                cd distribution/server-dist/target/
                mv "${APP}"-?.?.?*.tar.gz "${APP}-${params.VERSION}.tar.gz"
                curl --fail -u"${USR}:${PWD}" -T "${APP}-${params.VERSION}.tar.gz" --keepalive-time 2 "${REPO_URL}/${APP}-${params.VERSION}.tar.gz"
              """
            }
            if (!env.TAG_NAME || env.TAG_NAME != params.VERSION) {
              def git_url = "${env.GIT_URL}".replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)","")
              withCredentials([usernamePassword(credentialsId: "bgu",
                  passwordVariable: 'PWD',
                  usernameVariable: 'USR')]) {
                sh("git config --global user.email 'ci@dev.null'")
                sh("git config --global user.name 'ci'")
                sh("git tag ${VERSION} -m 'CI'")
                sh("git push https://${USR}:${PWD}@${git_url} --tags")
              }
            } else {
              echo "Tag ${env.TAG_NAME} already exists. Skipping."
            }
            echo "release ${VERSION} available at ${REPO_URL}/${APP}-${params.VERSION}.tar.gz"
          }
        }
      }
    }
  }
}
