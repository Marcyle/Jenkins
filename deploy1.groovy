#!/usr/bin/env groovy Jenkinsfile

node {
    try {
        def gradleHome = tool 'gradle3.5'
        stage('代码检出') {
            git branch: 'docker-sit', credentialsId: 'gitlab', url: 'http://10.10.0.xx/xx/xx.git'
        }
        stage('代码编译') {
            sh "'${gradleHome}/bin/gradle' clean build -x test"
            archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
        }
        def regPrefix = '10.41.0.206/xx/'
        stage('镜像构建') {
            docker.withRegistry('http://10.41.0.xx/','0fa2a5a1-42f9-424d-863b-37a9c7c93ede'){
                if ("${MODULE}".contains('api-gateway')){
                    def imageName = docker.build("${regPrefix}api-gateway:V2.0-${env.BUILD_ID}",'api-gateway')
                    imageName.push("V2.0-${env.BUILD_ID}")
                    imageName.push("latest")
                    sh "/usr/bin/docker rmi ${regPrefix}api-gateway:V2.0-${env.BUILD_ID}"
                }
                if ("${MODULE}".contains('eureka-server')){
                    def imageName = docker.build("${regPrefix}eureka-server:V2.0-${env.BUILD_ID}",'eureka-server')
                    imageName.push("V2.0-${env.BUILD_ID}")
                    imageName.push("latest")
                    sh "/usr/bin/docker rmi ${regPrefix}eureka-server:V2.0-${env.BUILD_ID}"
                }
                ... ...
                ... ...
            }
        }
        stage('集群部署') {
            docker.withServer('tcp://10.41.0.206:2375'){
                sh "docker pull ${regPrefix}api-gateway:latest && docker pull ${regPrefix}eureka-server:latest && docker pull ${regPrefix}gps-dataserver:latest && docker pull ${regPrefix}sdk-dataserver:latest"
            }
            docker.withServer('tcp://10.41.0.204:2375'){
                sh "docker pull ${regPrefix}api-gateway:latest && docker pull ${regPrefix}eureka-server:latest && docker pull ${regPrefix}gps-dataserver:latest && docker pull ${regPrefix}sdk-dataserver:latest"
            }
            docker.withServer('tcp://10.41.0.205:2375'){
                sh "docker pull ${regPrefix}api-gateway:latest && docker pull ${regPrefix}eureka-server:latest && docker pull ${regPrefix}gps-dataserver:latest && docker pull ${regPrefix}sdk-dataserver:latest"
                if ("${MODULE}".contains('eureka-server')){
                    sh "docker stack deploy -c compose/eureka-compose.yml bdc"
                }else {
                    sh "docker stack deploy -c compose/services-compose.yml bdc"
                }
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
            body: "<b style='font-size:12px'>(本邮件是程序自动下发的，请勿回复，<span style='color:red'>请相关人员fix it,重新提交到git 构建</span>)<br></b><hr><b style='font-size: 12px;'>项目名称：${JOB_NAME}<br></b><hr><b style='font-size: 12px;'>构建编号：${BUILD_NUMBER}<br></b><hr><b style='font-size: 12px;'>构建状态：${currentBuild.result}<br></b><hr><b style='font-size: 12px;'>构建日志地址：<a href='${BUILD_URL}console'>${BUILD_URL}console</a><br></b><b style='font-size: 12px;'>构建日志附件：请查看邮件附件！</a><br></b><hr>",
            recipientProviders: [[$class: 'DevelopersRecipientProvider']],
            to: "邮箱地址"
    )
}
