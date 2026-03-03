folder('demo-folder') {
    properties {
        folderLibraries {
            libraries {
                libraryConfiguration {
                    name('demo-lib')
                    defaultVersion('main')
                    implicit(false)
                    allowVersionOverride(true)

                    retriever {
                        modernSCM {
                            scm(class: 'hudson.plugins.git.GitSCM') {
                                userRemoteConfigs {
                                    userRemoteConfig {
                                        url('https://github.com/jenkinsci/job-dsl-plugin.git')
                                    }
                                }
                                branches {
                                    branchSpec {
                                        name('*/main')
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
