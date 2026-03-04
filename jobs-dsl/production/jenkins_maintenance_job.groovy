pipelineJob('admin/Jenkins_Maintenance_Job') {
    description('Job to manage Jenkins maintenance tasks like clearing queue, stopping jobs, and restarting Jenkins.')
    
    parameters {
        stringParam('SERVER', 'sfci02, sfci03, sfci04, sfci05, sfci07, sfci08, sfci10', 'Enter server names (comma-separated)')
        choiceParam('MODE', ['MAINTENANCE_MODE', 'REMOVE_MAINTENANCE_MODE', 'CLEAR_QUEUE_AND_ALL_RUNNING_JOBS', 'MAINTENANCE_MODE_AND_CLEAR_QUEUE_AND_ALL_RUNNING_JOBS', 'RESTART_JENKINS'], 'Select mode of operation')
        stringParam('CONFIRM', '', 'Type "CONFIRM" to confirm putting Jenkins in maintenance mode(required only for MAINTENANCE_MODE)')
    }

    definition {
        cps {
            script("""
pipeline {
    agent any

    environment {
        // Groovy script for clearing queue and stopping running jobs
        GROOVY_SCRIPT = '''
        // println(Jenkins.instance.pluginManager.plugins)
        import java.util.ArrayList
        import hudson.model.*
        import jenkins.model.Jenkins

          // Remove everything which is currently queued
          def q = Jenkins.instance.queue
          for (queued in Jenkins.instance.queue.items) {
            q.cancel(queued.task)
          }
        
          // Stop all the currently running jobs
          for (job in Jenkins.instance.items) {
            stopJobs(job)
          }
        
          def stopJobs(job) {
            if (job instanceof com.cloudbees.hudson.plugins.folder.Folder) {
              for (child in job.items) {
                stopJobs(child)
              }    
            } else if (job instanceof org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject) {
              for (child in job.items) {
                stopJobs(child)
              }
            } else if (job instanceof org.jenkinsci.plugins.workflow.job.WorkflowJob) {
              if (job.isBuilding()) {
                for (build in job.builds) {
                  build.doKill()
                }
              }
            }
          }
        '''
    }

    stages {
        stage('Validation') {
            steps {
                script {
                    if (params.CONFIRM != 'CONFIRM' && (params.MODE == 'MAINTENANCE_MODE' || 
                        params.MODE == 'CLEAR_QUEUE_AND_ALL_RUNNING_JOBS' || 
                        params.MODE == 'MAINTENANCE_MODE_AND_CLEAR_QUEUE_AND_ALL_RUNNING_JOBS')) {
                        error('You must type "CONFIRM" to confirm putting Jenkins in maintenance mode or clearing Jenkins queue and removing existing jobs in Jenkins')
                    }
                }
            }
        }

        stage('Call Jenkins API') {
            steps {
                script {
                    def servers = params.SERVER.split(',').collect { it.trim() }
                    for (def server : servers) {
                        withCredentials([usernamePassword(credentialsId: "\${server}-escape-hatch", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                            // Debugging: Print server and credentials
                            //echo "Processing server: \${server}"
                            //echo "Using credentials: \${USERNAME}"

                            // Get Jenkins crumb
                            def crumbResponse = sh(script: "curl -s -X GET https://\${server}.example.com/crumbIssuer/api/json -u \$USERNAME:\$PASSWORD", returnStdout: true).trim()
                            def crumbJson = readJSON text: crumbResponse
                            echo "Crumb received: \${crumbJson.crumb}"

                            if (params.MODE == 'MAINTENANCE_MODE') {
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD 'https://\${server}.example.com/quietDown' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} is now in maintenance mode."
                            } else if (params.MODE == 'REMOVE_MAINTENANCE_MODE') {
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD 'https://\${server}.example.com/cancelQuietDown' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} is now out of maintenance mode."
                            } else if (params.MODE == 'CLEAR_QUEUE_AND_ALL_RUNNING_JOBS') {
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD --data-urlencode 'script=\${GROOVY_SCRIPT}' \\
                                    'https://\${server}.example.com/scriptText' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} successfully cleared the queue and stopped all running jobs."
                            } else if (params.MODE == 'MAINTENANCE_MODE_AND_CLEAR_QUEUE_AND_ALL_RUNNING_JOBS') {
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD 'https://\${server}.example.com/quietDown' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} is now in maintenance mode."
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD --data-urlencode 'script=\${GROOVY_SCRIPT}' \\
                                    'https://\${server}.example.com/scriptText' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} successfully cleared the queue and stopped all running jobs."
                            } else if (params.MODE == 'RESTART_JENKINS') {
                                sh "curl -X POST -u \$USERNAME:\$PASSWORD --data-urlencode 'script=Jenkins.instance.safeRestart()' \\
                                    'https://\${server}.example.com/scriptText' -H 'Jenkins-Crumb: \${crumbJson.crumb}'"
                                echo "Jenkins - \${server} restart command issued."
                            } else {
                                error("Invalid MODE parameter: \${params.MODE}")
                            }
                        }
                    }
                }
            }
        }
    }
}
            """.stripIndent())
            sandbox()
        }
    }
}
