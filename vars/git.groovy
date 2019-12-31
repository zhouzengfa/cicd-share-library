#!groovy

/**
 * checkout 数据到指定目录
 * @param repoUrl git地址
 * @param branch  git 分支
 * @param credentialId git 账号
 * @param toSubDir  check到的目录
 * @return
 */
def checkOut(String repoUrl, String branch, String credentialId, String toSubDir="")
{
    sh("echo +++++++++++++++++++++++++ Start Git CheckOut +++++++++++++++++++++++++++++")

    echo "starting fetch code......"
    echo "repo: ${repoUrl}"
    echo "branch: ${branch}"
    echo "credentialId: ${credentialId}"
    echo "toSubDir: ${toSubDir}"

    if (toSubDir.size() > 0)
    {
        checkout([$class: 'GitSCM', branches: [[name: "${branch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${toSubDir}"]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "${credentialId}", url: "${repoUrl}"]]])
    }
    else
    {
        git branch: "${branch}", credentialsId: "${credentialId}", url: "${repoUrl}"
    }
    sh ('ls -l')

    sh("echo +++++++++++++++++++++++++ End Git CheckOut +++++++++++++++++++++++++++++")
}

/**
 * 提交指定目录下更新到Git
 * @param message 提交信息
 * @param dir  操作目录 默认为当前目录
 * @return
 */
def push(message, dir="")
{
    sh("echo +++++++++++++++++++++++++ Start Git Push +++++++++++++++++++++++++++++")

    echo "message: ${message}"
    echo "dir: ${dir}"

    sh """
        if [ "${dir}"x = "x" ]; then
            cd ${dir}
        fi
        
        git add -A
        git commit -m ${message}
        git push
    """

    sh("echo +++++++++++++++++++++++++ End Git Push +++++++++++++++++++++++++++++")
}