def call(String appGitUrlConf, String envTarget) {
    node('master-old') {
    // node('master') {

    //     agent = utility.defineAgent(JOB_URL)

    //     if (agent == "NOTDEFINED") {
    //         log.error "No agent defined for the target environment"
    //         currentBuild.result = 'ABORTED'
    //         error('No agent defined: ' + e.toString())
    //     } 
    //     else {
    //         println "Pipeline will run on " + agent
    //     }
    // }

    // node (agent) {

        String nexusCredentials = "azure-registry-rest-api"
        String dockerRegistry = "postesviluppo.azurecr.io"

        stage("Docker Login") {
            dockerUtility.dockerLogin(nexusCredentials, dockerRegistry)
        }


        def helmCommand = "docker run --rm --network=host -v \$(pwd):/apps -v ~/.helm:/root/.helm -v ~/.config/helm:/root/.config/helm -v ~/.cache/helm:/root/.cache/helm ${env.HELM_VERSION}"

        String gitCredentials = "utenza_jenkins"
        String packagePath = "posteitaliane." + params.CHART_RELEASE_NAME
        String ocpServiceToken = "ocp_service_token_" + params.NAMESPACE.toUpperCase()
        String repoPrefix = ""
        String version = params.VERSION_TO_INSTALL
        String chartName = params.CHART_NAME
        String chartReleaseName = params.CHART_RELEASE_NAME
	    String versionSplit=utility.getVersionSplitAmbiente("${VERSION_TO_INSTALL}")


        String timeOut = ""
        if (params.TIMEOUT == null || params.TIMEOUT == "") {
            timeOut = "300"
        }
        else {
            timeOut = params.TIMEOUT
        }
        def configPath = params.EXTERNAL_CONFIG
        
        
        String workingdir = "${WORKSPACE}/${packagePath}/workdir"

        stage('CleanWS') {
            deleteDir()
        }

        

        stage("Add Helm repository") {
            if (!envTarget.equals("SVILUPPO")) {
                helmUtility.addHelmRepoWithDocker(helmCommand, "posteitaliane", "https://nexus.alm.poste.it/repository/helm_repo_prod")
                repoPrefix = "posteitaliane"
            }
            else {
                helmUtility.addHelmRepoWithDocker(helmCommand, "posteitaliane-svil", "https://nexus.alm.poste.it/repository/helm_repo")
                repoPrefix = "posteitaliane-svil"
            }
        }

        stage("Repo search") {
            helmUtility.searchHelmRepoWithDocker(helmCommand, repoPrefix)
        }

        stage("Helm pull package") {
            helmUtility.runHelmPullDocker(helmCommand, chartName, workingdir, versionSplit, repoPrefix, envTarget)
        }

        stage('Download Configurazione') {
            if (!envTarget.equals("SVILUPPO")) {
                utility.sourceCheckout("${VERSION_TO_INSTALL}", gitCredentials, appGitUrlConf, "${WORKSPACE}/${packagePath}/workdir/${chartName}/extra")
            }
            else {
                utility.sourceCheckout("develop", gitCredentials, appGitUrlConf, "${WORKSPACE}/${packagePath}/workdir/${chartName}/extra")
            }
        }
	
        currentBuild.displayName = "${params.CHART_RELEASE_NAME}-${VERSION_TO_INSTALL}"
        currentBuild.description = "${NAMESPACE}"

        stage("Untar chart") {
            dir("${workingdir}") {
                sh(script: "tar -xvf ${chartName}-${versionSplit}.tgz")
            }
        }


        stage("Config overwrite") {
            if ( configPath != null && configPath != "") {
                dirList = configPath.tokenize(";")
                dirList.each{
                    dir("${workingdir}/${chartName}") {
                        sh(script: "cp -rf extra/${it} .")
                        sh(script: "ls -la extra/${it}")
                    }
                }
            }
        }


        stage('Helm deploy') {
            helmUtility.runHelmDeployFromVersionWithConf(helmCommand,ocpServiceToken, "${OCP_CLUSTER_URL}", chartReleaseName, "${NAMESPACE}", "${workingdir}/${chartName}", "extra/${PATH_CONF}", versionSplit, timeOut, envTarget)
        }

    //    stage('Run SmokeTest') {
    //         helmUtility.runSmokeTest("${OCP_CLUSTER_URL}", ocpServiceToken, "${CHART_RELEASE_NAME}", "${NAMESPACE}")
    //     }
    }
}

