#!/usr/bin/env groovy Jenkinsfile

node {
    try {
        stage('编译获取'){
            if ("${MODULE}".contains('front')){
                git credentialsId: 'gitlab', url: 'http://10.10.0.xx/xx/xx.git'
            }else{
                git credentialsId: 'gitlab', url: 'http://10.10.1.xx/xx/xx.git'
            }
        }
        def jenkins_ip = 'http://10.10.2.xx'
        def sdkPrefix = "${jenkins_ip}/job/项目名称/lastSuccessfulBuild/artifact/"
        def plarformPrefix = "${jenkins_ip}/job/项目名称/lastSuccessfulBuild/artifact/"
        stage('JAR文件获取'){
            if ("${MODULE}".contains('api-gateway')){
                sh "cd api-gateway && /usr/bin/curl -X GET -s -O ${sdkPrefix}api-gateway/build/libs/api-gateway-0.0.2-SNAPSHOT.jar --user admin:uUp2CYUN"
            }
            if ("${MODULE}".contains('eureka-server')){
                sh "cd eureka-server && /usr/bin/curl -X GET -s -O ${sdkPrefix}eureka-server/build/libs/eureka-server-0.0.2-SNAPSHOT.jar --user admin:uUp2CYUN"
            }
            ... ...
            ... ...
            if ("${MODULE}".contains('front')){
                echo "前端略过"
            }
        }
        def regPrefix = 'registry-vpc.cn-shenzhen.aliyuncs.com/仓库名/'
        stage('镜像构建') {
            if ("${MODULE}".contains('front')){
                withNPM(npmrcConfig:'npmrc') {
                    sh '/bin/cp -ar /tmp/xwsj/node_modules .'
                    sh 'npm install'
                    sh 'npm run build'
                    sh 'rm -rf node_modules'
                }
            }else{
                if ("${MODULE}".contains('api-gateway')){
                    def imageName = docker.build("${regPrefix}api-gateway:latest",'api-gateway')
                }
                if ("${MODULE}".contains('eureka-server')){
                    def imageName = docker.build("${regPrefix}eureka-server:latest",'eureka-server')
                }
                if ("${MODULE}".contains('gps-dataserver')){
                    def imageName = docker.build("${regPrefix}gps-dataserver:latest",'gps_data_server')
                }
                ... ...
                ... ...
            }
        }
        stage('镜像推送') {
            if ("${MODULE}".contains('front')){
                sh "rsync -rlgoDuvzP -e 'ssh -p 10022' $WORKSPACE/dist/ bqadm@10.41.0.xx:/prdoss/"
            }else{
                input message: '确认进行生产上传吗', ok: '确认', submitter: 'admin'
                if ("${MODULE}".contains('api-gateway')){
                    sh "/usr/bin/docker push ${regPrefix}api-gateway:latest"
                }
                if ("${MODULE}".contains('eureka-server')){
                    sh "/usr/bin/docker push ${regPrefix}eureka-server:latest"
                }
                if ("${MODULE}".contains('gps-dataserver')){
                    sh "/usr/bin/docker push ${regPrefix}gps-dataserver:latest"
                }
                ... ...
                ... ...
            }
        }
    } catch (any) {
        currentBuild.result = 'FAILURE'
        notifyFailed()
        throw any
    }
}

def notifyFailed() { 
    emailext (
            attachLog: true,
            subject: "构建通知:$JOB_NAME - Build # ${BUILD_NUMBER} - ${currentBuild.result}!",
            body: "<b style='font-size:12px'>(本邮件是程序自动下发的，请勿回复，<span style='color:red'>请相关人员fix it,重新提交到git 构建</span>)<br></b><hr><b style='font-size: 12px;'>项目名称：${JOB_NAME}<br></b><hr><b style='font-size: 12px;'>构建编号：${BUILD_NUMBER}<br></b><hr><b style='font-size: 12px;'>构建状态：${currentBuild.result}<br></b><hr><b style='font-size: 12px;'>最后提交：手动执行<br></b><hr><b style='font-size: 12px;'>构建日志地址：<a href='${BUILD_URL}console'>${BUILD_URL}console</a><br></b><b style='font-size: 12px;'>构建日志附件：请查看邮件附件！</a><br></b><hr>",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            to: "邮箱地址"
    )
}
