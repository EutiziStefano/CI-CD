import java.text.SimpleDateFormat

pipeline {
  agent {label 'master-old'}
  options {
      buildDiscarder(logRotator(numToKeepStr: '5'))
  }

  stages {
    
    stage('validation') {
      steps {
        script {
          if (!"${TEMPLATE_VERSION_TAG}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('TEMPLATE_VERSION_TAG is empty')
          }
          if (!"${TEMPLATE_CONF_GIT_URL}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('TEMPLATE_CONF_GIT_URL is empty')
          }
          if (!"${PROPERTIES_CONF_GIT_URL}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('PROPERTIES_CONF_GIT_URL is empty')
          }
          if (!"${OCP_CLUSTER_URL}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('OCP_CLUSTER_URL is empty')
          }
          if (!"${OCP_SERVICE_TOKEN}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('OCP_SERVICE_TOKEN is empty')
          }
          if (!"${APP}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('APP is empty')
          }
          if (!"${NAMESPACE}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('NAMESPACE is empty')
          }
          if (!"${AMBIENTE}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('AMBIENTE is empty')
          }
          if (!"${GIT_CREDENTIAL_ID}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('GIT_CREDENTIAL_ID is empty')
          }

        }
      }
    }
    stage('CleanWS') {
     steps{
      script {
      deleteDir()
     }
   }
  }
         
    stage('Configuration checkout') {
      steps {
        checkout(
          [$class           : 'GitSCM', branches: [[name: "${TEMPLATE_VERSION_TAG}"]], doGenerateSubmoduleConfigurations: false,
          extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'svilrepo']],
          submoduleCfg     : [],
          userRemoteConfigs: [[credentialsId: "${GIT_CREDENTIAL_ID}", url: "${TEMPLATE_CONF_GIT_URL}"]]]
        )
        checkout(
          [$class           : 'GitSCM', branches: [[name: "*/master"]], doGenerateSubmoduleConfigurations: false,
          extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'confrepo']],
          submoduleCfg     : [],
          userRemoteConfigs: [[credentialsId: "${GIT_CREDENTIAL_ID}", url: "${PROPERTIES_CONF_GIT_URL}"]]]
        )
      }
    }
		 
    stage('OCP install helm chart') {
      steps {
        script {
          withCredentials([string(credentialsId: "$OCP_SERVICE_TOKEN", variable: 'SECRET')]) {
            try {
              sh 'oc whoami --token=$SECRET --server=$OCP_CLUSTER_URL --insecure-skip-tls-verify'    
              sh 'cd ${WORKSPACE}/svilrepo/chart/${APP}/ && oc login $OCP_CLUSTER_URL --token=$SECRET --namespace=${NAMESPACE} --insecure-skip-tls-verify && helm upgrade --install ${APP} -f ${WORKSPACE}/confrepo/${AMBIENTE}/${TEMPLATE_VERSION_TAG}/${AMBIENTE}_${APP}.values .'
            }
            catch (Exception e) {
              echo e.getMessage()
              error('Conclusa con errore.')
            }
          }
        }
      }
    }
  
  }
}
