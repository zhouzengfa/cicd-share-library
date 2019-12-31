#!groovy

/**
 *  * 推镜像到内网仓库
 * @param imageName 镜像命名 包括版本信息
 * @param registry 仓库
 * @param path  路径
 * @param imageName 镜像名字 包括版本信息
 * @param credential 仓库认证ID
 * @return
 */
def push(registry, path, imageName) {
    sh("echo +++++++++++++++++++++++++ Start Docker Push +++++++++++++++++++++++++++++")

    echo "starting push image:${imageName}"
    targetImageName = "${registry}/${path}/${imageName}"
    sh """
        docker tag ${imageName} ${targetImageName}
        docker push ${targetImageName}
        #docker rmi -f ${targetImageName}
       """

    sh("echo +++++++++++++++++++++++++ End Docker Push +++++++++++++++++++++++++++++")
}

/**
 * 构建镜像 此接口需要在与Dockerfile同级目录下调用
 * @param imageName 镜像名字 要包括仓库及版本信息
 * @return
 */
def build(imageName, dockerfile="Dockerfile")
{
    sh("echo +++++++++++++++++++++++++ Start Docker Build +++++++++++++++++++++++++++++")

    echo "start build image: ${imageName}"
    echo "dockerfile: ${dockerfile}"
    sh("docker build -t ${imageName} -f ${dockerfile} .")

    sh("echo +++++++++++++++++++++++++ End Docker Build +++++++++++++++++++++++++++++")
}
/**
 * 根据pom.xml产生镜像名字
 * @param pomDir pom.xml目录，相对与调用此接口的目录
 * @return
 */
def getImageName(pomDir)
{
    sh("echo +++++++++++++++++++++++++ Start GetImageName +++++++++++++++++++++++++++++")
    sh("echo pomDir:${pomDir}")

    if (pomDir.size() > 0)
    {
        gitCommitId= sh(script:"cd ${pomDir}; git rev-parse --short HEAD", returnStdout:true).trim()
        pom = readMavenPom file: "${pomDir}/pom.xml"
    }
    else
    {
        gitCommitId=sh(script:"git rev-parse --short HEAD;", returnStdout: true).trim()
        pom = readMavenPom file: "pom.xml"
    }

    version = pom.getVersion()
    artifactId = pom.getArtifactId()

    time=sh(script: "date \"+%Y.%m.%d.%H%M%S\"", returnStdout: true).trim()
    imageName= "${artifactId}:${version}.git.${gitCommitId}.build.${time}"
    echo imageName

    sh("echo +++++++++++++++++++++++++ End GetImageName +++++++++++++++++++++++++++++")

    return imageName
}

/**
 * 所有镜像仓库初始化login
 * @return
 */
def loginAllRegistry(Map config)
{
    setDefaultParams(config)
    withCredentials([usernamePassword(credentialsId: "${config.yunWeiRegistryCredId}", passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
        sh """
            docker login --username ${REGISTRY_USERNAME}  --password ${REGISTRY_PASSWORD} ${config.yunWeiRegistry}
       """
    }

    withCredentials([usernamePassword(credentialsId: "${config.devRegistryCredId}", passwordVariable: 'REGISTRY_PASSWORD', usernameVariable: 'REGISTRY_USERNAME')]) {
        sh """
            docker login --username ${REGISTRY_USERNAME}  --password ${REGISTRY_PASSWORD} ${config.devRegistry}
       """
    }
}