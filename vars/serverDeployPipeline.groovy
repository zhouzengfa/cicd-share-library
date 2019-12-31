#!groovy

// 服务器部署 Pipeline
def call(Map config) {
    print(config)
    setDefaultParams(config)
    def deployNamespace = 'server'
    def region='Undefined'
    pipeline {
        agent { label 'PlatformBuilder' }
        parameters {
            string(name: 'imageTag', defaultValue: '1.0.0.git.a11cf2f.build.v20181225.193004', description: '镜像Tag', trim: true)
            choice(name: 'region',choices:'街坊棋牌(jfqp)\n贵州贵阳(gzgy)', description: '地区')
            string(name: 'apolloCluster', defaultValue: 'general', description: 'Apollo集群', trim: true)
        }

        options {
            //保持构建的最大个数
            buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '30', numToKeepStr: '10')
            disableConcurrentBuilds()
        }

        //pipeline的各个阶段场景
        stages {
            stage('初始化') {
                steps {
                    script {
                        dock.loginAllRegistry(config)
                        region = params.region.substring(params.region.indexOf('(')+1, params.region.indexOf(')'))
                    }
                }
            }
            stage("获取Chats") {
                steps {
                    script {
                        git.checkOut("git@git.loho.local:loho-games/server-game/tools/JenkinsUsedScript.git", "master", "${config.devGitCredId}")
                    }
                }
            }
            stage("部署服务器") {
                steps {
                    sh """
                    running=`helm list | grep ${region} | wc -l`
                    if [ $running lt 0 ]; then
                        helm del --purge ${region}
                        echo "region: ${region} deleted"
                    fi
                    helm install --name ${region} --namespace ${deployNamespace} deploy-game --set=image.region=${region},image.tag=${params.imageTag}
                  """
                }
            }

            stage('清理工作区') {
                steps {
                    cleanWs()
                }
            }
        }
   }
}

