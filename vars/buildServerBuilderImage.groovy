#!groovy

// 构建ServerBuilder工具镜像
def call()
{
    def config = [:]
    setDefaultParams(config)

    pipeline {
        agent {label 'ServerBuilder'}
//        agent any
        parameters {
            string(name: 'version', defaultValue: '1.0.0', description: '版本', trim: true)
        }

        environment {
            SOURCE = 'Source'
            IMAGE_NAME="server-builder:${version}"
        }

        options {
            //保持构建的最大个数
            buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '30', numToKeepStr: '10')
            disableConcurrentBuilds()
        }

        stages {
            stage('Init'){
                steps {
                    script {
                        dock.loginAllRegistry()
                    }
                }
            }
            stage('CheckOut') {
                steps {
                    script {
                        git.checkOut("git@git.loho.local:loho-games/server-game/tools/JenkinsUsedScript.git", "master", "${config.devGitCredId}", "${SOURCE}")
                    }
                }
            }
            stage('构建镜像') {
                steps {
                    dir("${SOURCE}/tools/server-builder") {
                        script {
                            dock.build(env.IMAGE_NAME)
                        }
                    }
                }
            }
            stage('推送到内网仓库') {
                steps {
                    script {
                        dock.push(config.devRegistry, "jenkinsci", env.IMAGE_NAME)
                    }
                }
            }
            stage('清理工作区') {
                steps {
                    cleanWs()
                }
            }
        }

        post {
            success {
                echo 'this is success!!!'
            }
            failure {
                echo 'this is failure!!!'
            }
        }
    }
}

