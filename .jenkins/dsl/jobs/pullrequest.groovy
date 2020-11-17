import org.kie.jenkins.jobdsl.templates.KogitoJobTemplate
import org.kie.jenkins.jobdsl.KogitoConstants
import org.kie.jenkins.jobdsl.Utils

folder(KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER)

Map defaultJobParams = [
    job: [
        name: 'kogito-runtimes',
        folder: KogitoConstants.KOGITO_DSL_PULLREQUEST_FOLDER
    ],
    git: [
        author: "${GIT_AUTHOR_NAME}",
        branch: "${GIT_BRANCH}",
        repository: 'kogito-runtimes',
        credentials: "${GIT_AUTHOR_CREDENTIALS_ID}",
        token_credentials: "${GIT_AUTHOR_TOKEN_CREDENTIALS_ID}"
    ]
]

// Default Build&Test PR check job
def prCheckParams = Utils.deepCopyObject(defaultJobParams)
KogitoJobTemplate.createPRJob(this, prCheckParams)

// DSL check job
def dslCheckParams = Utils.deepCopyObject(defaultJobParams)
dslCheckParams.job.name = dslCheckParams.job.name + '-dsl'
dslCheckParams.pr = [ commitContext : 'Check DSL' ]
dslCheckParams.jenkinsfile = '.jenkins/Jenkinsfile.pr.dsl-check'
KogitoJobTemplate.createPRJob(this, dslCheckParams)