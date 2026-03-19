#!/usr/bin/env groovy
import java.text.SimpleDateFormat

def currentDate = new Date()
String dateTime = currentDate.format("dd.MM.yyyy HH:mm")

pipeline {
    agent {
        label 'dev'
    }
    parameters {
        choice(name: 'whichScript', choices: ['bash', 'python'], description: 'Wybierz skrypt ktory uruchomimy')
        string(name: 'city', defaultValue: 'Warsaw', description: 'Wprowadz nazwe miasta do sprawdzenia pogody')
        string(name: 'myApiKey', defaultValue: '7cff9972d5c98a9a47ddf6a59cb34d8e', description: 'Wprowadz klucz API do pogody')
    }

    stages {
        stage('Wybrales bash') {
            when {
                expression { params.whichScript == 'bash' }
            }
            steps {
                sh """
                    bash ./cwiczenia/pogoda/start.sh '${city}' '${myApiKey}'
                    echo 'Podana temperatura w ${city} jest z dnia i godziny ${dateTime}'
                """
            }
        }
        stage('Test bash') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                echo 'Udalo sie wykonac skrypt pogoda'
            }
        }
        stage('Wybrales python') {
            when {
               expression { params.whichScript == 'python' }
            }
            steps {
                sh """
                    python3 ./cwiczenia/pogoda_python/start.py '${city}' '${myApiKey}'
                    echo 'Podana temperatura w ${city} jest z dnia i godziny ${dateTime}'
                """
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}