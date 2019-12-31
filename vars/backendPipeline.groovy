#!groovy

// 后台打包Pipeline
def call(Map config)
{
    print(config)
    if (config["sourceDir"] == null)
    {
        config["sourceDir"]="Source"
    }

    if (config["sourceCredId"] == null)
    {
        config["sourceCredId"]="3243c332-d32b-4313-a45c-785de4d69d33"
    }

    if (config["dockerDir"] == null)
    {
        config["dockerDir"]="Docker"
    }

    if (config["dockerCredId"] == null)
    {
        config["dockerCredId"]='f2cf15fc-889b-4e80-ad0b-7cec81da7f89'
    }

    pipeline {
        //在任何可用的代理上执行Pipeline
//        agent {label 'PlatformBuilder'}
        agent any
        parameters {
            //若勾选在pipelie完成后会邮件通知测试人员进行验收
            booleanParam(name: 'isCommitQA', description: '是否邮件通知测试人员进行人工验收', defaultValue: false)
        }

        environment {
            IMAGE_REPO_CRED_ID = '22d65943-4aa1-45ba-ad1f-3fc89d86fe82'
        }

        //环境变量，初始确定后一般不需更改
        tools {
            maven 'maven'
            // jdk   'jdk8'
        }

        options {
            //保持构建的最大个数
            buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '30', numToKeepStr: '10')
            disableConcurrentBuilds()
        }

        //pipeline的各个阶段场景
        stages {
            stage('Init'){
                dock.loginAllRegistry()
            }
            stage('获取代码') {
                steps {
                    gitCheckOut("${config.sourceUrl}", "${config.sourceBranch}", "${config.sourceCredId}", "${config.sourceDir}");
                }
            }
            stage('获取Dockerfile') {
                steps {
                    gitCheckOut("${config.dockerUrl}", "${config.dockerBranch}", "${config.dockerCredId}", "${config.dockerDir}");
                }
            }
            stage('Maven打包') {
                steps {
                    dir("${config.sourceDir}") {
                        sh 'mvn clean package'
                    }
                }
            }
            stage('构建镜像') {
                steps {
                    script {
                        imageName = getImageName("${config.sourceDir}")
                        echo imageName
                    }
                    dir("${config.dockerDir}") {
                        sh ("cp ../${config.sourceDir}/${config.packagePath} ./lib/ -r")
                        dockerBuild(imageName)
                    }
                }
            }
            stage('推送到内网仓库') {
                steps {
                    dockerPush(imageName, env.IMAGE_REPO_CRED_ID)
                }
            }
            stage('清理工作区') {
                steps {
                    cleanWs()
                }
            }
        }

        //pipeline运行结果通知给触发者
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

