import java.text.SimpleDateFormat

def target_cluster_flags = ""
def version = ""
def appname = ""

pipeline {
 agent {label 'slave2 || slave3'}
  stages {    
   stage('CleanWS') {
    steps{
     script {
      deleteDir()
    }
   }
  }

  stage('prepare') {
   steps {
    script {
     if (!"${BUILD_BRANCH}"?.trim()) {
         currentBuild.result = 'ABORTED'
         error('Tag to build is empty')
     }
         echo "Releasing branch ${BUILD_BRANCH}"
         target_cluster_flags = "--server=$ocp_cluster_url --insecure-skip-tls-verify"
   }
  }
 }

  stage('Source checkout') {
   steps {
    checkout(
     [$class                           : 'GitSCM', branches: [[name: "${BUILD_BRANCH}"]],
      doGenerateSubmoduleConfigurations: false,
      extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${WORKSPACE}/${PACKAGE_PATH}"]],
      submoduleCfg                     : [],
      userRemoteConfigs                : [[credentialsId: "${GIT_CREDENTIAL_ID}", url: "${APP_GIT_URL}"]]])
    }
   }

  stage('Get version from chart') {
      steps{
       script{
        appname = "${IMAGE_NAME}".substring( "${IMAGE_NAME}".indexOf("/") + 1)
        echo "App Name: ${appname}"
        
        chart = readYaml file: "${WORKSPACE}/${PACKAGE_PATH}/chart/Chart.yaml"
        version = chart.appVersion
        
        echo "Chart App Version: ${chart.appVersion}"
    }
   }
  }     

  stage('Docker File') {
   steps{
    sh "cd  ${WORKSPACE}/${PACKAGE_PATH} && docker build  --build-arg NO_PROXY=gitlab.alm.poste.it --build-arg HTTP_PROXY=http://10.204.68.5:8080 --build-arg HTTPS_PROXY=http://10.204.68.5:8080 --tag ${IMAGE_NAME} -f ${DOCKER_FILE} ."
    }
   }

  stage('Docker TAG') {
   steps {
    script {
     try{
      def updateResult = sh(script: "NO_PROXY=gitlab.alm.poste.it HTTP_PROXY=http://10.204.68.5:8080 HTTPS_PROXY=http://10.204.68.5:8080 docker tag ${IMAGE_NAME}:latest ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}", returnStdout: true)
      echo updateResult;
        }catch(e){
          echo "Error in Create TAG:"+e.toString()
        }
       }
      }
     }

  stage('Docker Login') {
   steps {
    script {
     try{
      withCredentials([usernamePassword(credentialsId: "$NEXUS_CREDENTIALS", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]){
       def updateResult = sh(script: "NO_PROXY=gitlab.alm.poste.it HTTP_PROXY=http://10.204.68.5:8080 HTTPS_PROXY=http://10.204.68.5:8080 docker login ${DOCKER_REGISTRY} -u $USERNAME -p $PASSWORD ", returnStdout: true)
       echo updateResult;
          }
         }catch(e){
           echo "Error in Login:"+e.toString()
         }
        }
       }
	  }

  stage('Docker Push') {
   steps {
    script {
     try{
      def updateResult = sh(script: "NO_PROXY=gitlab.alm.poste.it HTTP_PROXY=http://10.204.68.5:8080 HTTPS_PROXY=http://10.204.68.5:8080 docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}", returnStdout: true)
      echo updateResult;
        }catch(e){
          echo "Error in Pull:"+e.toString()
        }
       }
      }
     }
     
    
    stage('validation') {
      steps {
        script {
          if (!"${OCP_CLUSTER_URL}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('OCP_CLUSTER_URL is empty')
          }
          if (!"${OCP_SERVICE_TOKEN}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('OCP_SERVICE_TOKEN is empty')
          }
          if (!"${NAMESPACE}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('NAMESPACE is empty')
          }
          if (!"${GIT_CREDENTIAL_ID}"?.trim()) {
            currentBuild.result = 'ABORTED'
            error('GIT_CREDENTIAL_ID is empty')
          }

          target_cluster_flags = "--server=$OCP_CLUSTER_URL --insecure-skip-tls-verify"
        }
      }
    }
             
    stage('Configuration checkout') {
      steps {
        checkout(
          [$class           : 'GitSCM', branches: [[name: "${BUILD_BRANCH}"]], doGenerateSubmoduleConfigurations: false,
          extensions       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'svilrepo']],
          submoduleCfg     : [],
          userRemoteConfigs: [[credentialsId: "${GIT_CREDENTIAL_ID}", url: "${APP_GIT_URL}"]]]
        )
      }
    }
    
    stage('OCP apply Helm3 Chart') {
      steps {
        script {
          withCredentials([string(credentialsId: "${OCP_SERVICE_TOKEN}", variable: 'SECRET')]) {
            try {
              sh(script: "cd ${WORKSPACE}/svilrepo/chart/ && oc login $OCP_CLUSTER_URL --token=$SECRET --namespace=${NAMESPACE} --insecure-skip-tls-verify && helm upgrade --install ${appname} .", returnStdout: true)
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
