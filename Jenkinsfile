node('android'){
    env.JAVA_HOME = ("${tool name: 'jdk-latest', type: 'jdk'}")
    env.PATH = ("${env.JAVA_HOME}/bin:${env.PATH}")

    stage('clean up'){
        cleanWs()
    }

    stage('scm'){
        checkout scm
    }

    stage('build and publish'){
        sh("chmod +x gradlew")
        sh("./gradlew :MPChartLib:uploadArchives --stacktrace")
    }
}