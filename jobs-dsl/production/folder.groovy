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
                        legacySCM {
                            scm {
                                git {
                                    remote {
                                        url('https://github.com/jenkinsci/job-dsl-plugin.git')
                                    }
                                    branch('*/main')
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
