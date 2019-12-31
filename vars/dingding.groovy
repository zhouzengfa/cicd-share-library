#!groovy

/**
 * 钉钉通知
 * @param isSuccess 是否构建成功
 * @param tips 提示
 * @param detailUrl 详情url
 * @return
 */
def notify(boolean isSuccess, String tips, String detailUrl) {
    def text="${tips}\n[详情...](${detailUrl}/console)"
    if (isSuccess) {
        text="**构建成功**\n${text}"
    } else {

        text="**构建失败**\n${text}"
    }
    sh """
       curl 'https://oapi.dingtalk.com/robot/send?access_token=a9f6839c4b91242cd061d0902ef65b8096d4ba5ae5227d57f908b0864182ef39' \
       -H 'Content-Type: application/json' \
       -d '
       {
           "msgtype": "markdown",
           "markdown": {"title":"镜像构建",
           "text":"${text}"}
       }'
     """
}
