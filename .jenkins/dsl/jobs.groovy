import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils
import org.kie.jenkins.jobdsl.KogitoJobType

def getDefaultJobParams() {
    return [
        job: [
            name: 'kogito-runtimes'
        ],
        git: [
            author: "${GIT_AUTHOR_NAME}",
            branch: "${GIT_BRANCH}",
            repository: 'kogito-runtimes',
            credentials: "${GIT_AUTHOR_CREDENTIALS_ID}",
            token_credentials: "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}"
        ],
        env: [:],
        pr: [:]
    ]
}

def getJobParams(String jobName, String jenkinsfileName, String jobDescription = '') {
    def jobParams = getDefaultJobParams()
    jobParams.job.name = jobName
    jobParams.jenkinsfile = jenkinsfileName
    if (jobDescription) {
        jobParams.job.description = jobDescription
    }
    return jobParams
}

Map getMultijobPRConfig() {
    return [
        parallel: true,
        jobs : [
            [
                id: 'Runtimes',
                primary: true,
            ], [
                id: 'Optaplanner',
                dependsOn: 'Runtimes',
                repository: 'optaplanner'
            ], [
                id: 'Apps',
                dependsOn: 'Optaplanner',
                repository: 'kogito-apps'
            ], [
                id: 'Examples',
                dependsOn: 'Optaplanner',
                repository: 'kogito-examples'
            ]
        ]
    ]
}

if (Utils.isMainBranch()) {
    // Old PR checks. To be removed once supported release branches (<= 1.7.x) are no more there.
    setupPrJob()
    setupQuarkusLTSPrJob()
    setupNativePrJob()

    // PR checks
    setupMultijobPrDefaultChecks()
    setupMultijobPrNativeChecks()
    setupMultijobPrLTSChecks()
}

KogitoJobTemplate.setupDeployJob(this) { return getDefaultJobParams() }
KogitoJobTemplate.setupPromoteJob(this) { return getDefaultJobParams() }

// Nightly jobs
if (Utils.isMainBranch()) {
    setupNightlyDroolsJob()

    setupNightlyQuarkusJob('main')
    setupNightlyQuarkusJob("${QUARKUS_LTS_VERSION}")
}
setupNightlySonarCloudJob()
setupNightlyNativeJob()

/////////////////////////////////////////////////////////////////
// Methods
/////////////////////////////////////////////////////////////////

void setupPrJob() {
    def jobParams = getDefaultJobParams()
    jobParams.pr.whiteListTargetBranches = ['1.5.x', '1.7.x']
    jobParams.env = [ TIMEOUT_VALUE : 240 ]
    KogitoJobTemplate.createPRJob(this, jobParams)
}

void setupQuarkusLTSPrJob() {
    def jobParams = getDefaultJobParams()
    jobParams.pr.whiteListTargetBranches = ['1.5.x', '1.7.x']
    jobParams.env = [ TIMEOUT_VALUE : 240 ]
    KogitoJobTemplate.createQuarkusLTSPRJob(this, jobParams)
}

void setupNativePrJob() {
    def jobParams = getDefaultJobParams()
    jobParams.pr.whiteListTargetBranches = ['1.5.x', '1.7.x']
    jobParams.env = [ TIMEOUT_VALUE : 600 ]
    KogitoJobTemplate.createNativePRJob(this, jobParams)
}

void setupMultijobPrDefaultChecks() {
    KogitoJobTemplate.createMultijobPRJobs(this, getMultijobPRConfig()) {
        def jobParams = getDefaultJobParams()
        jobParams.pr.blackListTargetBranches = ['1.5.x', '1.7.x']
        return jobParams
    }
}

void setupMultijobPrNativeChecks() {
    KogitoJobTemplate.createMultijobNativePRJobs(this, getMultijobPRConfig()) {
        def jobParams = getDefaultJobParams()
        jobParams.pr.blackListTargetBranches = ['1.5.x', '1.7.x']
        return jobParams
    }
}

void setupMultijobPrLTSChecks() {
    KogitoJobTemplate.createMultijobLTSPRJobs(this, getMultijobPRConfig()) {
        def jobParams = getDefaultJobParams()
        jobParams.pr.blackListTargetBranches = ['1.5.x', '1.7.x']
        return jobParams
    }
}

void setupNightlyDroolsJob() {
    def jobParams = getJobParams('kogito-drools-snapshot', 'Jenkinsfile.drools', 'Kogito Runtimes Drools Snapshot')
    jobParams.triggers = [ cron : 'H 2 * * *' ]
    KogitoJobTemplate.createNightlyJob(this, jobParams).with {
        parameters {
            stringParam('BUILD_BRANCH_NAME', "${GIT_BRANCH}", 'Set the Git branch to checkout')
            stringParam('GIT_AUTHOR', "${GIT_AUTHOR_NAME}", 'Set the Git author to checkout')

            stringParam('DROOLS_VERSION', '', '(optional) If not set, then it will be guessed from drools repository')
            stringParam('DROOLS_REPOSITORY', '', '(optional) In case Drools given version is in a specific repository')
        }
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }
}

void setupNightlyQuarkusJob(String quarkusBranch) {
    def jobParams = getJobParams("kogito-quarkus-${quarkusBranch}", 'Jenkinsfile.quarkus', 'Kogito Runtimes Quarkus Snapshot')
    jobParams.triggers = [ cron : 'H 4 * * *' ]
    KogitoJobTemplate.createNightlyJob(this, jobParams).with {
        parameters {
            stringParam('BUILD_BRANCH_NAME', "${GIT_BRANCH}", 'Set the Git branch to checkout')
            stringParam('GIT_AUTHOR', "${GIT_AUTHOR_NAME}", 'Set the Git author to checkout')
        }
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
            env('QUARKUS_BRANCH', quarkusBranch)
        }
    }
}

void setupNightlySonarCloudJob() {
    def jobParams = getJobParams('kogito-runtimes-sonarcloud', 'Jenkinsfile.sonarcloud', 'Kogito Runtimes Daily Sonar')
    jobParams.triggers = [ cron : 'H 20 * * 1-5' ]
    KogitoJobTemplate.createNightlyJob(this, jobParams).with {
        parameters {
            stringParam('BUILD_BRANCH_NAME', "${GIT_BRANCH}", 'Set the Git branch to checkout')
            stringParam('GIT_AUTHOR', "${GIT_AUTHOR_NAME}", 'Set the Git author to checkout')
        }
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }
}

void setupNightlyNativeJob() {
    def jobParams = getJobParams('kogito-native', 'Jenkinsfile.native', 'Kogito Runtimes Native Testing')
    jobParams.triggers = [ cron : 'H 6 * * *' ]
    KogitoJobTemplate.createNightlyJob(this, jobParams).with {
        parameters {
            stringParam('BUILD_BRANCH_NAME', "${GIT_BRANCH}", 'Set the Git branch to checkout')
            stringParam('GIT_AUTHOR', "${GIT_AUTHOR_NAME}", 'Set the Git author to checkout')
        }
        environmentVariables {
            env('JENKINS_EMAIL_CREDS_ID', "${JENKINS_EMAIL_CREDS_ID}")
        }
    }
}
