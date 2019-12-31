#!groovy

// 设置默认参数
def call(Map config) {
    print(config)

    // 开发用的Git仓库权限
    if (config["devGitCredId"] == null) {
        config["devGitCredId"] = "8b81e530-7dd9-4ab4-8ecf-0803a9dfa645"
    }

    // 打包好的服务器Package Git地址
//    if (config["serverPackegeGitUrl"] == null) {
//        config["serverPackegeGitUrl"] = "git@git.loho.local:loho-games/release/server-game.git"
//    }

    // 运维用的Git仓库权限
    if (config["yunWeiGitCredId"] == null) {
        config["yunWeiGitCredId"] = "28cf7a60-88ed-43f8-875a-ef88b3f79b77"
    }

    // 开发用的内网镜像仓库
    if (config["devRegistry"] == null) {
        config["devRegistry"] = "registry.loho.local"
    }

    // 开发用的网镜像仓库权限
    if (config["devRegistryCredId"] == null) {
        config["devRegistryCredId"] = '22d65943-4aa1-45ba-ad1f-3fc89d86fe82'
    }

    // 运维用的镜像仓库
    if (config["yunWeiRegistry"] == null) {
        config["yunWeiRegistry"] = "ccr.ccs.tencentyun.com"
    }

    // 运维用的镜像仓库权限
    if (config["yunWeiRegistryCredId"] == null) {
        config["yunWeiRegistryCredId"] = 'b927da64-becc-4cfd-9874-6242ed6d619c'
    }

    // 运维用的镜像前辍
    if (config["yunWeiImagePrefix"] == null) {
        config["yunWeiImagePrefix"] = 't19-mahjong-'
    }


    // 打包好的服务器Package Git地址
//    if (config["serverDockerfileGitUrl"] == null) {
//        config["serverDockerfileGitUrl"] = "git@gitlab.itops.lohogames.com:loho-games/release/docker-team/mahjong-server/mahjong-server-dockerfile.git"
//    }
}
