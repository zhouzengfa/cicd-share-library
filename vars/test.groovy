/*需要配置参数
1.String 参数 Version 版本

2.choice 参数 Operation 操作
列表：
	Start
	Close
	Restart
3.String 参数 Area 区
*/
def gameServers = [interface:"InterfaceServer",game:"GameServer", auth:"AuthServer", club:"ClubServer",battle:"BattleServer"/*,campaign:"CampaignServer", replay:"ReplayServer"*/]
pipeline {
    agent any
    environment {
        CREDENTIALS_ID = '8b81e530-7dd9-4ab4-8ecf-0803a9dfa645'
        ROOT_DIR=pwd()
        AREA="${Area}"
        CLUSTER="${Cluster}"
        //FOO = credentials("6cc69a88-41d5-4d9e-9a6d-ad97fbbcf493	")

    }
    stages{
        stage('Checkout Script') {
            steps{
                git([url: 'git@git.loho.local:loho-games/server-game/tools/JenkinsUsedScript.git', branch: 'master', credentialsId: "${CREDENTIALS_ID}"])
            }
        }
        stage('Preparing ...') {
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                                echo "pwd"
                                 #修改镜像版本 
                                sed -i "s@loho/server/${entry.key}:.*@loho/server/${entry.key}:${Version}@g" ./"${entry.key}"/jenkins/template.yaml
                                #修改deployment、container、service的名字
                                sed -i "s@name:.*loho-${entry.key}@name: loho-${entry.key}-${AREA}@g" ./"${entry.key}"/jenkins/template.yaml
                                #修改area标签名
                                sed -i "s@area:.*test@area: ${AREA}@g" ./"${entry.key}"/jenkins/template.yaml
                                # whnt修改aq
                                sed -i "s@k8sTest@${AREA}@g" ./"${entry.key}"/jenkins/template.yaml
                                #修改Cluster集群
                                sed -i "s@default@${CLUSTER}@g" ./"${entry.key}"/jenkins/template.yaml
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Check') {
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                            case "${Operation}" in
                            Start)
                                if [ `kubectl get deployment loho-${entry.key}-${AREA} | wc -l` -gt 0 ]; then
                                    echo [ERROR] deployment loho-${entry.key}-${AREA} is running. close first please!
                                    exit 1
                                fi
                                ;;
                            Restart)
                                if [ `kubectl get deployment loho-${entry.key}-${AREA} | grep loho-${entry.key}-${AREA}|wc -l` -ne 1 ]; then
                                    echo "[WARN] deployment loho-${entry.key}-${AREA} is not running, can't be restarted!!!"
                                    exit 1
                                fi
                                ;;
                            Close)
                                if [ `kubectl get deployment loho-${entry.key}-${AREA} | wc -l` -eq 0 ]; then
                                    echo "[WARN] deployment loho-${entry.key}-${AREA} is not running, can't be closed!!!"
                                    #exit 1
                                fi
                                ;;
                            *)
                                echo "can't handle. area=${AREA},node=${entry.key} operation:{"${Operation}"}".
                                ;;
                            esac
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Close') {
            when {
                beforeAgent true
                expression {"${Operation}" == "Close"}
            }
            options {
                timeout(time: 2, unit: 'MINUTES')
            }
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                               if [ `kubectl delete -f ./${entry.key}/jenkins/template.yaml | grep deleted` ];then
                                    echo close ${entry.key} success.
                                fi
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Start') {
            when {
                beforeAgent true
                expression {"${Operation}" == "Start"}
            }
            options {
                timeout(time: 2, unit: 'MINUTES')
            }
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                             kubectl create -f ./${entry.key}/jenkins/template.yaml
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Restart') {
            when {
                beforeAgent true
                expression {"${Operation}" == "Restart"}
            }
            options {
                timeout(time: 2, unit: 'MINUTES')
            }
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                                kubectl set image deployment/loho-${entry.key}-${AREA} loho-${entry.key}-${AREA}=registry.loho.local:5000/loho/server/${entry.key}:${Version} --namespace=default
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Wating') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            steps{
                script{
                    def builds = [:]
                    gameServers.each{ entry ->
                        builds[entry.key]={
                            sh """
                            count=0
                            while [ true ]
                            do
                                case "${Operation}" in
                                Start|Restart)
                                    if [ `kubectl get pod -l area="${AREA}",node="${entry.key}" | grep Running | wc -l` -gt 0 ]; then
                                        if [ "${entry.key}" == "auth" ]; then
                                            if [ ! `kubectl logs deployment/loho-auth-${AREA} | grep openPortForGmt` ];then
                                                echo wating auth up finish....
                                                continue
                                            fi
                                            echo auth up finish. open white list.
                                            sleep 5
                                            resutl=`kubectl get pod -l area=${AREA},node=auth | grep loho-auth | sed 's/[ ].*//g'  | xargs -I {}  kubectl exec -i {} -- curl http://localhost:5000/gmtools?%7B%22server_id%22%3A%2017104897%2C%20%22handler%22%3A%20%22SetWhiteListEnableHandler%22%2C%20%22enabled%22%3A%20%220%22%7D`
                                        elif [ "${entry.key}" == "interface" ]; then
                                            if [ ! `kubectl logs deployment/loho-interface-${AREA} | grep "begin to start self"` ];then
                                                echo wating interface up finish....
                                                continue
                                            fi
                                            echo interface up finish. enjoy now
                                        fi
                                        echo Started
                                        break
                                    else
                                        echo -e "* /c" 
                                    fi
                                    ;;
                                Close)
                                    if [ `kubectl get pod -l area=${AREA},node=${entry.key} | wc -l` -gt 0 ]; then
                                        echo wating ${entry.key} close
                                    else
                                        echo ${entry.key} close success.
                                        break
                                    fi
                                    ;;
                                *)
                                    echo "[ERROR]can't handle operation:{"${Operation}"}."
                                    exit 1
                                    ;;
                                esac
                            sleep 1s
                            done
                            """
                        }
                    }
                    parallel builds
                }
            }
        }
        stage('Success') {
            steps{
                cleanWs()
                sh "echo done"
            }
        }

    }
}