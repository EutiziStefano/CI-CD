import jenkins.model.*

/*  PARAMETRI PIPELINE
CARTELLA_CONTENITORE
APPLICATION
NAMESPACE
OCP_CLUSTER_URL
OCP_SERVICE_TOKEN
TEMPLATE_CONF_GIT_URL
PROPERTIES_CONF_GIT_URL
*/

def jobName = "${APPLICATION}"+"_helm"
def parentfolder = "${CARTELLA_CONTENITORE}"

String[] ocpclusters="${OCPclusterURLs}".split("\\r?\\n")
String[] ocptokens="${OCPJenkinsTokens}".split("\\r?\\n")
def ienv=0

//for(env in ["CERTIFICAZIONE"]){
for(env in ["COLLAUDO", "CERTIFICAZIONE", "PRODUZIONE"]){
  String[] str;
  str = parentfolder.split('/');

  def mkdir
  def path="/"+env
      
  for (i = 0; i < str.length; i++) {
      mkdirp=Jenkins.instance.getItemByFullName(path)
      path+="/"+str[i]
      mkdirc=Jenkins.instance.getItemByFullName(path)
      if ( mkdirc == null ) {
         mkdirp.createProject(com.cloudbees.hudson.plugins.folder.Folder.class, str[i])
      }
  }

  def dovecreare = Jenkins.instance.getItemByFullName(env+"/"+parentfolder)

  def configXml = "<?xml version='1.1' encoding='UTF-8'?> <flow-definition plugin='workflow-job@2.24'> <actions> <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin='pipeline-model-definition@1.3.2'/> <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin='pipeline-model-definition@1.3.2'> <jobProperties> <string>jenkins.model.BuildDiscarderProperty</string> </jobProperties> <triggers/> <parameters/> <options/> </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction> </actions> <description></description> <keepDependencies>false</keepDependencies> <properties> <hudson.model.ParametersDefinitionProperty> <parameterDefinitions> <com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition plugin='extended-choice-parameter@0.76'> <name>TEMPLATE_VERSION_TAG</name> <description>Versione da deployare</description> <quoteValue>false</quoteValue> <saveJSONParameterToFile>false</saveJSONParameterToFile> <visibleItemCount>5</visibleItemCount> <type>PT_SINGLE_SELECT</type> <groovyScript>import groovy.json.JsonSlurper&#13;&#10;&#13;&#10;List&lt;String&gt; artifacts = new ArrayList&lt;String&gt;()&#13;&#10;&#13;&#10;try {&#13;&#10;   &#13;&#10;    def artifactsUrl = &quot;http://10.195.181.30/nexus/repository/docker-registry/v2/&quot;+app+&quot;/tags/list&quot;        &#13;&#10;    def artifactsObjectRaw = [&quot;curl&quot;, &quot;-s&quot;, &quot;-H&quot;, &quot;accept: application/json&quot;, &quot;-k&quot;, &quot;--url&quot;, &quot;\${artifactsUrl}&quot;].execute().text&#13;&#10;&#13;&#10;\tdef jsonSlurper = new JsonSlurper()&#13;&#10;    def artifactsJsonObject = jsonSlurper.parseText(artifactsObjectRaw)&#13;&#10;    def dataArray = artifactsJsonObject.tags&#13;&#10;    for(item in dataArray){&#13;&#10;\t\tif (!item.endsWith(&apos;-SNAPSHOT&apos;)){&#13;&#10;\t\t\tartifacts.add(item)&#13;&#10;\t\t}&#13;&#10;    } &#13;&#10;&#13;&#10;} catch (Exception e) {&#13;&#10;    artifacts.add(&quot;There was a problem fetching the artifacts&quot;)&#13;&#10;artifacts.add(e)&#13;&#10;}&#13;&#10;return artifacts</groovyScript> <bindings>app=$APPLICATION</bindings> <groovyClasspath></groovyClasspath> <multiSelectDelimiter>,</multiSelectDelimiter> </com.cwctravel.hudson.plugins.extended__choice__parameter.ExtendedChoiceParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>TEMPLATE_CONF_GIT_URL</name> <description></description> <defaultValue>$TEMPLATE_CONF_GIT_URL</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>PROPERTIES_CONF_GIT_URL</name> <description></description> <defaultValue>$PROPERTIES_CONF_GIT_URL</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>OCP_CLUSTER_URL</name> <description></description> <defaultValue>"+ocpclusters[ienv]+"</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>OCP_SERVICE_TOKEN</name> <description></description> <defaultValue>"+ocptokens[ienv]+"</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>NAMESPACE</name> <description></description> <defaultValue>$NAMESPACE</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>GIT_CREDENTIAL_ID</name> <description></description> <defaultValue>utenza_jenkins</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>APP</name> <description></description> <defaultValue>$APPLICATION</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> <com.wangyin.parameter.WHideParameterDefinition plugin='hidden-parameter@0.0.4'> <name>AMBIENTE</name> <description></description> <defaultValue>$env</defaultValue> </com.wangyin.parameter.WHideParameterDefinition> </parameterDefinitions> </hudson.model.ParametersDefinitionProperty> <jenkins.model.BuildDiscarderProperty> <strategy class='hudson.tasks.LogRotator'> <daysToKeep>-1</daysToKeep> <numToKeep>5</numToKeep> <artifactDaysToKeep>-1</artifactDaysToKeep> <artifactNumToKeep>-1</artifactNumToKeep> </strategy> </jenkins.model.BuildDiscarderProperty> <io.fabric8.jenkins.openshiftsync.BuildConfigProjectProperty plugin='openshift-sync@1.0.27'> <uid></uid> <namespace></namespace> <name></name> <resourceVersion></resourceVersion> </io.fabric8.jenkins.openshiftsync.BuildConfigProjectProperty> <org.datadog.jenkins.plugins.datadog.DatadogJobProperty plugin='datadog@0.7.1'> <emitOnCheckout>false</emitOnCheckout> </org.datadog.jenkins.plugins.datadog.DatadogJobProperty> <com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty plugin='gitlab-plugin@1.5.9'> <gitLabConnection>JENKINS_SVILUPPO</gitLabConnection> </com.dabsquared.gitlabjenkins.connection.GitLabConnectionProperty> </properties> <definition class='org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition' plugin='workflow-cps@2.61.1'> <scm class='hudson.plugins.git.GitSCM' plugin='git@3.9.1'> <configVersion>2</configVersion> <userRemoteConfigs> <hudson.plugins.git.UserRemoteConfig> <url>https://gitlab.rete.poste/digital/pipeline.git</url> <credentialsId>utenza_jenkins</credentialsId> </hudson.plugins.git.UserRemoteConfig> </userRemoteConfigs> <branches> <hudson.plugins.git.BranchSpec> <name>*/master</name> </hudson.plugins.git.BranchSpec> </branches> <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations> <submoduleCfg class='list'/> <extensions/> </scm> <scriptPath>helm_pipeline.groovy</scriptPath> <lightweight>true</lightweight> </definition> <triggers/> <disabled>false</disabled> </flow-definition>"

  def xmlStream = new ByteArrayInputStream(configXml.getBytes())
  dovecreare.createProjectFromXML(jobName, xmlStream)
  
  ienv=ienv+1
}
