#!/usr/bin/groovy

pipeline {
    agent any
    tools {
        maven 'maven'
        jdk 'openjdk8'
    }

    parameters {
        booleanParam(defaultValue: false, description: 'Skal prosjektet releases? Hvis ikke blir det bare tagget som nytt snapshot', name: 'isRelease')
        string(name: "releaseVersion", defaultValue: "", description: "Hva er det nye versjonsnummeret (X.X.X)? Som default releases snapshot-versjonen")
    }

    stages {
        stage('Initialize') {
            steps {
                sh '''
                     echo "PATH = ${PATH}"
                     echo "M2_HOME = ${M2_HOME}"                       
                 '''
            }
        }

        stage('generate api') {
            steps {

                sh './generateFeignClient.sh'
            }
        }

        stage('Build') {
            steps {
                script {
                    def pom = readMavenPom file: 'pom.xml'
                    env.POM_VERSION = pom.version
                }
                sh 'mvn -U -B clean install'
            }
        }

        stage('Snapshot: verify pom') {
            when {
                expression { !params.isRelease }
            }

            steps {
                sh "mvn enforcer:enforce@validate-snap"
            }
        }

        stage('Release: new version') {
            when {
                expression { params.isRelease }
            }

            steps {
                script {
                    if (params.releaseVersion == null || params.releaseVersion == "")
                        releaseVersion = env.POM_VERSION.replace("-SNAPSHOT", "")

                }
                gitCheckout(pipelineParams.branch)
                prepareRelease releaseVersion
                gitTag(isRelease, releaseVersion)
            }
        }


        stage('Deploy artifacts') {
            steps {
                sh 'mvn -U -B -Dmaven.install.skip=true deploy'
            }
        }

        stage('Release: set snapshot') {
            when {
                expression { params.isRelease }
            }

            steps {
                setSnapshot releaseVersion
                gitPush()
            }
        }
    }
}
