import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification

class JenkinsfilePromote extends JenkinsPipelineSpecification {
	def Jenkinsfile = null

    def setup() {
        Jenkinsfile = loadPipelineScriptForTest("Jenkinsfile.promote")
        Jenkinsfile.getBinding().setVariable("PROPERTIES_FILE_NAME", "deployment.properties")
    }

    //////////////////////////////////////////////////////////////////////////////
    // Deployment properties
    //////////////////////////////////////////////////////////////////////////////

	def "[Jenkinsfile.promote] readDeployProperties: no DEPLOY_BUILD_URL parameter" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['DEPLOY_BUILD_URL' : ''])
		when:
			Jenkinsfile.readDeployProperties()
		then:
			0 * getPipelineMock("sh")("wget artifact/deployment.properties -O deployment.properties")
            0 * getPipelineMock("readProperties").call(['file':'deployment.properties'])
	}

	def "[Jenkinsfile.promote] readDeployProperties: DEPLOY_BUILD_URL parameter" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['DEPLOY_BUILD_URL' : 'https://www.google.ca/'])
		when:
			Jenkinsfile.readDeployProperties()
		then:
			1 * getPipelineMock("sh")("wget https://www.google.ca/artifact/deployment.properties -O deployment.properties")
            1 * getPipelineMock("readProperties").call(['file':'deployment.properties'])
	}

	def "[Jenkinsfile.promote] hasDeployProperty: deployProperties has" () {
		setup:
            Jenkinsfile.getBinding().setVariable("deployProperties", ['foo' : 'bar'])
		when:
			def has = Jenkinsfile.hasDeployProperty('foo')
		then:
            has == true
	}

	def "[Jenkinsfile.promote] hasDeployProperty: deployProperties does not have" () {
		setup:
            Jenkinsfile.getBinding().setVariable("deployProperties", [:])
		when:
			def has = Jenkinsfile.hasDeployProperty('foo')
		then:
            has == false
	}

	def "[Jenkinsfile.promote] getDeployProperty: deployProperties has" () {
		setup:
            Jenkinsfile.getBinding().setVariable("deployProperties", ['foo' : 'bar'])
		when:
			def fooValue = Jenkinsfile.getDeployProperty('foo')
		then:
            fooValue == 'bar' 
	}

	def "[Jenkinsfile.promote] getDeployProperty: deployProperties does not have" () {
		setup:
            Jenkinsfile.getBinding().setVariable("deployProperties", [:])
		when:
			def fooValue = Jenkinsfile.getDeployProperty('foo')
		then:
            fooValue == ""
	}

	def "[Jenkinsfile.promote] getParamOrDeployProperty: param" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['FOO' : 'BAR'])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['foo' : 'bar'])
		when:
			def fooValue = Jenkinsfile.getParamOrDeployProperty('FOO', 'foo')
		then:
            fooValue == "BAR"
	}

	def "[Jenkinsfile.promote] getParamOrDeployProperty: deploy property" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['FOO' : ''])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['foo' : 'bar'])
		when:
			def fooValue = Jenkinsfile.getParamOrDeployProperty('FOO', 'foo')
		then:
            fooValue == "bar"
	}

    //////////////////////////////////////////////////////////////////////////////
    // Getter / Setter
    //////////////////////////////////////////////////////////////////////////////

	def "[Jenkinsfile.promote] isRelease: only RELEASE param true" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['RELEASE' : true])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['release' : false])
		when:
			def release = Jenkinsfile.isRelease()
		then:
            release == true
	}

	def "[Jenkinsfile.promote] isRelease: only deploy property true" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['RELEASE' : false])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['release' : true])
		when:
			def release = Jenkinsfile.isRelease()
		then:
            release == true
	}

	def "[Jenkinsfile.promote] isRelease: both true" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['RELEASE' : true])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['release' : true])
		when:
			def release = Jenkinsfile.isRelease()
		then:
            release == true
	}

	def "[Jenkinsfile.promote] isRelease: both false" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['RELEASE' : false])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['release' : false])
		when:
			def release = Jenkinsfile.isRelease()
		then:
            release == false
	}

	def "[Jenkinsfile.promote] getGitTag: GIT_TAG param" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['GIT_TAG' : 'tag', 'PROJECT_VERSION' : 'version'])
		when:
			def tag = Jenkinsfile.getGitTag()
		then:
            tag == 'tag'
	}

	def "[Jenkinsfile.promote] getGitTag: no GIT_TAG param" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['GIT_TAG' : '', 'PROJECT_VERSION' : 'version'])
		when:
			def tag = Jenkinsfile.getGitTag()
		then:
            tag == 'version'
	}

	def "[Jenkinsfile.promote] getBuildBranch: no BUILD_BRANCH_NAME parameter" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['BUILD_BRANCH_NAME' : ''])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['git.branch' : 'branch'])
		when:
			def branchName = Jenkinsfile.getBuildBranch()
		then:
            branchName == 'branch'
	}

	def "[Jenkinsfile.promote] getBuildBranch: BUILD_BRANCH_NAME parameter" () {
		setup:
            Jenkinsfile.getBinding().setVariable("params", ['BUILD_BRANCH_NAME' : 'param branch'])
            Jenkinsfile.getBinding().setVariable("deployProperties", ['git.branch' : 'branch'])
		when:
			def branchName = Jenkinsfile.getBuildBranch()
		then:
            branchName == 'param branch'
	}
}
