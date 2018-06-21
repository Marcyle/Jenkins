node {
    try {
        stage('代码拉取') {
            if ("${MODULE}".contains('frontend')){
                dir('frontend'){
                    git branch: 'master', credentialsId: '123456', url: 'http://10.10.0.xx/xx/xx.git'
                }
            }else{
                git branch: 'master', credentialsId: '123456', url: 'http://10.10.0.xx/xx/xx.git'
            }
        }
        stage('项目构建') {
            if ("${MODULE}".contains('frontend')){
                echo "前端无需构建"
            }else{
                withMaven(
                    maven: 'M3',
                    mavenSettingsConfig: 'mvnrc',
                    options: [
                    findbugsPublisher(disabled: false,isRankActivated: true),
                    artifactsPublisher(disabled: false)
                ]) {
                    sh "mvn -Dmaven.test.skip=true clean package"
                }
            }
        }
        stage('项目部署') {
            if ("${MODULE}".contains('backend')){
                sh "ansible CreditMarket-PRD -m synchronize -a 'mode=push src=$WORKSPACE/target/cw-manager-1.0-SNAPSHOT/ dest=/data/www/tomcat8080_api'"
            }else {
                sh "rsync -rlgoDuvzP -e 'ssh -p 10022' $WORKSPACE/src/main/resources/admin/dist/ www@10.41.1.xx:/data/www/xx"
                sh "rsync -rlgoDuvzP -e 'ssh -p 10022' $WORKSPACE/src/main/resources/admin/dist/ www@10.41.1.xx:/data/www/xx"

            }
        }
        stage('重启项目'){
            if ("${MODULE}".contains('backend')){
                sh "ansible CreditMarket-PRD -m service -a 'name=tomcat8080 state=restarted'"         
            }else{
                echo '前端略过'
            }
        }
    } catch (any) {
        currentBuild.result = 'FAILURE'
        throw any
    }
}
