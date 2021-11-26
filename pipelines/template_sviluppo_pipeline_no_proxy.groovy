import java.text.SimpleDateFormat

def target_cluster_flags = ""
def version = ""

pipeline {
 agent {label 'slave1 || slave2 || slave3'}
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

    stage('Maven Check'){
	 when{
	  expression{
	   file=sh(script: 'find . -name "pom.xml"', returnStdout: true).trim()
       echo file
	   return file != null && file != '';
	  }
	 }
      steps{
       script{
        pom = readMavenPom file: "${file}"
        version = pom.version
        echo "Version: ${pom.version}"
    }
   }
  }

/*
   stage('Gradle Check'){
    when{
     expression{
	  file=sh(script: 'find . -name "build.gradle"', returnStdout: true).trim()
	  echo file
	  return file != null && file != '';
	}
   }
      steps{
       script{
       	version1 =  sh(script: "cat ${file} | grep -o 'version = [^,]*'", returnStdout: true)
        version2 = version1.split(/=/)[1].trim()
		version = version2.split (/'/) [1].trim()
        echo "|"+version+"|"
    }
   }
  }

  stage('Golang Check'){
   when {
    expression {
	  path = sh """find . -name "*version.go" -exec dirname {} \\;"""
	  file=sh(script: 'find . -name "version.go"', returnStdout: true)
	  echo file
	  return file != null && file != '';
    }
   }
      steps{
       script{
       	version =  sh(script: 'grep string ${WORKSPACE}/${PACKAGE_PATH}/${path}/version.go|awk -F"\"" \'{print $2}\'|awk -F"\"" \'{print $1}\'', returnStdout: true)
        version = "$version".substring("$version".indexOf("\"")+1, "$version".lastIndexOf("\""))
        echo "|"+version+"|"
	}
   }
  }
  
  stage('DotNet Check'){
   when {
    expression {
      return params.CSPRJ_NAME !=null && params.CSPRJ_NAME != '';
    }
   }
   steps{
    script{
     version=sh(script: 'grep Version ${WORKSPACE}/${PACKAGE_PATH}/${CSPRJ_NAME}|awk -F">" \'{print $2}\'|awk -F"<" \'{print $1}\'', returnStdout:true)
	 version = "$version".trim()
	 echo version
    }
   }
  }


  stage('NodeJs Check'){
   when{
    expression{
     file=sh(script: 'find . -name "package.json"', returnStdout: true).trim()
	 echo file
	 return file != null && file != '';
	}
   }
    steps{
     script{
      props = readJSON file: "${file}"
	  version = props.version 
      echo version
        }
       }
      }
     }
    }
    
*/
  stage('Docker File') {
   steps{
    sh "cd  ${WORKSPACE}/${PACKAGE_PATH} && docker build --tag ${IMAGE_NAME} -f ${DOCKER_FILE} ."
    }
   }

  stage('Docker TAG') {
   steps {
    script {
     try{
      def updateResult = sh(script: "docker tag ${IMAGE_NAME}:latest ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}", returnStdout: true)
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
       def updateResult = sh(script: "docker login ${DOCKER_REGISTRY} -u $USERNAME -p $PASSWORD ", returnStdout: true)
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
      def updateResult = sh(script: "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${version}", returnStdout: true)
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
		 
    stage('OCP apply template') {
      steps {
        script {
          withCredentials([string(credentialsId: "${OCP_SERVICE_TOKEN}", variable: 'SECRET')]) {
            try {
              sh 'oc login --server=$OCP_CLUSTER_URL --token=$SECRET --insecure-skip-tls-verify'
              echo "Executing oc process template "
              sh 'oc process -f ${WORKSPACE}/svilrepo/openshift/Conf_sviluppo/Template/template-${IMAGE_NAME}.yml --param-file=${WORKSPACE}/svilrepo/openshift/Conf_sviluppo/Template/template-${IMAGE_NAME}.properties | oc apply -f - $target_cluster_flags --token=$SECRET --namespace=${NAMESPACE}'
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
