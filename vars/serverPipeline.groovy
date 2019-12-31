#!groovy

// 服务器打包 Pipeline
def call(Map config) {
    print(config)
    setDefaultParams(config)

    def regionConvert = [:]
    regionConvert['cn_hbcz'] = 'hbcz'
    regionConvert['cn_jfqp'] = 'jfqp'

    // 镜像版本
    def version = "1.0.0"
    def servers = ["Undefined"]
    def imageName = [:]
    def region=""
    def dingdingTips=""
    pipeline {
        agent { label 'PlatformBuilder' }
//        agent any
        parameters {
            string(name: 'serverBranch', defaultValue: 'guiyang_v20181212_185542', description: '打好包服务器分支', trim: true)
            string(name: 'serverList', defaultValue: 'auth,battle,campaign,club,game,interface,replay', description: '需要构建镜像的服务器列表，用逗号隔开', trim: true)
            string(name: 'region', defaultValue: 'gzgy', description: '地区', trim: true)

            //若勾选在pipelie完成后会邮件通知测试人员进行验收
            booleanParam(name: 'isNotifyQA', description: '是否通知QA进行验收', defaultValue: false)
        }

        environment {
            PACKAGE_DIR = "Package"
            DOCKER_FILE_DIR = "Dockerfile"
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
                        servers = params.serverList.tokenize(",");
                        region = params.region;
                        if (region.contains("_")) {
                            region = regionConvert[region]
                        }
                        dingdingTips = "#### 服务器分支："+params.serverBranch +"\n"
                        dingdingTips += "#### 地区："+params.region+"\n"
                    }
                }
            }
            stage("拉取构建数据") {
                failFast true
                parallel {
                    stage('获取服务器包') {
                        steps {
                            script {
                                git.checkOut("git@git.loho.local:loho-games/release/server-game.git", "${params.serverBranch}", "${config.devGitCredId}", "${PACKAGE_DIR}");
                            }
                        }
                    }
                    stage('获取Dockerfile') {
                        steps {
                            script {
                                git.checkOut("git@gitlab.itops.lohogames.com:loho-games/release/docker-team/mahjong-server/mahjong-server-dockerfile.git", "master",
                                        "${config.yunWeiGitCredId}", "${DOCKER_FILE_DIR}");
                            }
                        }
                    }
//                    stage('基础镜像拉取') {
//                        steps {
//                            sh 'docker pull ccr.ccs.tencentyun.com/loho_ten19_images/centos6-jdk:v1'
//                        }
//                    }
                }
            }
            stage('准备构建') {
                failFast true
                steps {
                    script {
                        // 计算镜像版本信息
                        def gitCommitId = sh(script: "cd ${PACKAGE_DIR}; git rev-parse --short HEAD;", returnStdout: true).trim()
//                        def time = sh(script: "date \"+%Y.%m.%d.%H%M%S\"", returnStdout: true).trim()
                        def time = params.serverBranch.substring(params.serverBranch.indexOf('_')+1, params.serverBranch.length()).replace('_', '.')
                        def imageVersion = "${version}.git.${gitCommitId}.build.${time}"

                        def builds = [:]
                        servers.each {
                            imageName[it] = "${it}:${imageVersion}"
                            builds[it] = {
                                sh """
                                # 包copy
                                cp ${PACKAGE_DIR}/${it} ${DOCKER_FILE_DIR}/${it}/lib/ -r; ls -l ${DOCKER_FILE_DIR}/${it}/lib/
                                # 测试环境meta和zookeeper替换
                                sed -i 's#meta.*#meta=http://fat-conf.apollo.mahjong.nbigame.com:3101#g' ${DOCKER_FILE_DIR}/${it}/lib/${it}/resource/apollo-fat.properties
                                sed -i 's/zk.address.*/zk.address=zk.address=test.zk01.mahjong.lohogames.com:2181,test.zk02.mahjong.lohogames.com:2181,test.zk03.mahjong.lohogames.com:2181/g' ${DOCKER_FILE_DIR}/${it}/lib/${it}/resource/apollo-fat.properties
                                # dev环境增加nodePath配置项
                                sed -i 's/nodePath.*//g' ${DOCKER_FILE_DIR}/${it}/lib/${it}/resource/apollo-dev.properties
                                echo -e "\nnodePath=default" >> ${DOCKER_FILE_DIR}/${it}/lib/${it}/resource/apollo-dev.properties
                                #基础镜像替换
                                #sed -i 's@ccr.ccs.tencentyun.com/loho_ten19_images/centos6-jdk:v1@registry.loho.local/base/centos6:v1@g' ${DOCKER_FILE_DIR}/${it}/Dockerfile
                                # 修改启动脚本 
                                sed -i -e":begin; /cmd.*=.*_str.join(list)/,/def.*proc_stop():/ { /def.*proc_stop():/! { \$! { N; b begin }; }; s/cmd.*=.*def.*proc_stop():/os.system(_str.join(list))\\n\\ndef proc_stop():/; };" ${
                                    DOCKER_FILE_DIR
                                }/${it}/lib/${it}/bin/*.py 
                            """
                            }
                        }
                        parallel builds
                    }
                }
            }
            stage('构建镜像') {
                failFast true
                steps {
                    script {
                        def builds = [:]
                        imageName.each { k, v ->
                            builds[k] = {
                                dir("${DOCKER_FILE_DIR}/${k}") {
                                    sh 'ls -l /lib/'
                                    dock.build(v)
                                }
                            }
                        }
                        parallel builds
                    }
                }
            }
            stage('推送到仓库') {
                failFast true
                parallel {
                    stage('推送到内网仓库') {
                        failFast true
                        steps {
                            script {
                                def builds = [:]
                                imageName.each { k, v ->
                                    builds[k] = {
                                        dock.push(config.devRegistry, "loho/" + region, v)
                                    }
                                }

                                parallel builds
                            }
                        }
                    }
                    stage('推送到外网仓库') {
                        when { expression { return params.isNotifyQA } }
                        failFast true
                        steps {
                            script {
                                dingdingTips += "#### 镜像列表：\n"
                                def builds = [:]
                                imageName.each { k, v ->
                                    builds[k] = {
                                        dock.push(config.yunWeiRegistry, config.yunWeiImagePrefix + region, v)
                                    }

                                    // 钉钉消息
                                    dingdingTips += " - "+config.yunWeiRegistry+"/"+config.yunWeiImagePrefix + region+"/"+v+"\n"
                                }

                                parallel builds
                            }
                        }
                    }
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
            always {
                cleanWs()
            }
            success {
                echo 'this is success!!!'
                script{
                    if (params.isNotifyQA) {
                        dingding.notify(true, dingdingTips, BUILD_URL)
                    }
                }
            }
            failure {
                script{
                    if (params.isNotifyQA) {
                        dingding.notify(false, dingdingTips, BUILD_URL)
                    }
                }
            }
        }
    }
}

