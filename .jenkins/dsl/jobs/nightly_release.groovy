import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

branchFolder = "${KogitoConstants.KOGITO_DSL_NIGHTLY_RELEASE_FOLDER}/${JOB_BRANCH_FOLDER}"

folder(KogitoConstants.KOGITO_DSL_NIGHTLY_RELEASE_FOLDER)
folder(branchFolder)

defaultJobParams = [
    job: [
        name: 'kogito-runtimes',
        folder: branchFolder
    ],
    git: [
        author: "${GIT_AUTHOR_NAME}",
        branch: "${GIT_BRANCH}",
        repository: 'kogito-runtimes',
        credentials: "${GIT_AUTHOR_CREDENTIALS_ID}",
        token_credentials: "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}"
    ]
]

def getJobParams(String jobName, String jobDescription, String jenkinsfileName){
    def jobParams = Utils.deepCopyObject(defaultJobParams)
    jobParams.job.name=jobName
    jobParams.job.description=jobDescription
    jobParams.jenkinsfile=".jenkins/${jenkinsfileName}"
    return jobParams
}

// Deploy pipeline
KogitoJobTemplate.createPipelineJob(this, getJobParams('kogito-runtimes-deploy', 'Kogito Runtimes Deploy', 'Jenkinsfile.deploy')).with {
    parameters {
        stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')

        // Build&test information
        booleanParam('SKIP_TESTS', false, 'Skip tests')

        // Release information
        booleanParam('RELEASE', false, 'Is this build for a release?')
        stringParam('PROJECT_VERSION', '', 'Optional if not RELEASE. If RELEASE, cannot be empty.')
    }

    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        
        env('GIT_BRANCH_NAME', "${GIT_BRANCH}")
        env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
        env('AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
        env('GITHUB_TOKEN_CREDS_ID', "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}")
        env('GIT_AUTHOR_BOT', "${GIT_BOT_AUTHOR_NAME}")
        env('BOT_CREDENTIALS_ID', "${GIT_BOT_AUTHOR_CREDENTIALS_ID}")

        env('NEXUS_RELEASE_URL', "${MAVEN_NEXUS_RELEASE_URL}")
        env('NEXUS_RELEASE_REPOSITORY_ID', "${MAVEN_NEXUS_RELEASE_REPOSITORY}")
        env('NEXUS_STAGING_PROFILE_ID', "${MAVEN_NEXUS_STAGING_PROFILE_ID}")
        env('NEXUS_BUILD_PROMOTION_PROFILE_ID', "${MAVEN_NEXUS_BUILD_PROMOTION_PROFILE_ID}")

        env('MAVEN_SETTINGS_CONFIG_FILE_ID', "${MAVEN_SETTINGS_FILE_ID}")
        env('MAVEN_DEPENDENCIES_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")
        env('MAVEN_DEPLOY_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")
    }
}


// Promote pipeline
KogitoJobTemplate.createPipelineJob(this, getJobParams('kogito-runtimes-promote', 'Kogito Runtimes Promote', 'Jenkinsfile.promote')).with {
    parameters {
        stringParam('DISPLAY_NAME', '', 'Setup a specific build display name')
        
        // Deploy job url to retrieve deployment.properties
        stringParam('DEPLOY_BUILD_URL', '', 'URL to jenkins deploy build to retrieve the `deployment.properties` file.')
        
        // Release information which can override `deployment.properties`
        booleanParam('RELEASE', false, 'Override `deployment.properties`. Is this build for a release?')
        stringParam('PROJECT_VERSION', '', 'Override `deployment.properties`. Optional if not RELEASE. If RELEASE, cannot be empty.')

        stringParam('GIT_TAG', '', 'Git tag to set, if different from PROJECT_VERSION')
    }

    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        
        env('GIT_BRANCH_NAME', "${GIT_BRANCH}")
        env('GIT_AUTHOR', "${GIT_AUTHOR_NAME}")
        env('AUTHOR_CREDS_ID', "${GIT_AUTHOR_CREDENTIALS_ID}")
        env('GITHUB_TOKEN_CREDS_ID', "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}")
        env('GIT_AUTHOR_BOT', "${GIT_BOT_AUTHOR_NAME}")
        env('BOT_CREDENTIALS_ID', "${GIT_BOT_AUTHOR_CREDENTIALS_ID}")

        env('MAVEN_SETTINGS_CONFIG_FILE_ID', "${MAVEN_SETTINGS_FILE_ID}")
        env('MAVEN_DEPENDENCIES_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")
        env('MAVEN_DEPLOY_REPOSITORY', "${MAVEN_ARTIFACTS_REPOSITORY}")
    }
}

// Sonarcloud
def sonarcloudJobParams = getJobParams('kogito-runtimes-sonarcloud', 'Kogito Runtimes Daily Sonar', 'Jenkinsfile.sonarcloud')
sonarcloudJobParams.triggers = [ cron : 'H 20 * * 1-5' ]
KogitoJobTemplate.createPipelineJob(this, sonarcloudJobParams).with {
    environmentVariables {
        env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
    }
}

if("${GIT_BRANCH}" == "${GIT_MAIN_BRANCH}") { 
    // Those jobs should be created only on main branch

    // Drools snapshot
    def droolsJobParams = getJobParams('kogito-drools-snapshot', 'Kogito Runtimes Drools Snapshot', 'Jenkinsfile.drools')
    droolsJobParams.triggers = [ cron : 'H 2 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, droolsJobParams).with {
        parameters {
            stringParam('BRANCH_NAME', "master", 'Set the branch to build&test')
        }

        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }

    // Quarkus snapshot
    def quarkusJobParams = getJobParams('kogito-quarkus-snapshot', 'Kogito Runtimes Quarkus Snapshot', 'Jenkinsfile.quarkus')
    quarkusJobParams.triggers = [ cron : 'H 4 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, quarkusJobParams).with {
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }


    // Native
    def nativeJobParams = getJobParams('kogito-native', 'Kogito Runtimes Native Testing', 'Jenkinsfile.native')
    nativeJobParams.triggers = [ cron : 'H 6 * * *' ]
    KogitoJobTemplate.createPipelineJob(this, nativeJobParams).with {
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }
}