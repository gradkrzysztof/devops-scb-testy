#!/usr/bin/env groovy
import java.text.SimpleDateFormat

def currentDate = new Date()
String dateTime = currentDate.format("dd.MM.yyyy HH:mm")

pipeline {
    agent {
        label 'dev'
    }
    parameters {
        choice(name: 'whichScript', choices: ['bash', 'python', 'python-flusk'], description: 'Wybierz skrypt ktory uruchomimy')
        string(name: 'city', defaultValue: 'Warsaw', description: 'Wprowadz nazwe miasta do sprawdzenia pogody')
        string(name: 'myApiKey', defaultValue: '7cff9972d5c98a9a47ddf6a59cb34d8e', description: 'Wprowadz klucz API do pogody')
        string(name: 'localImageName', defaultValue: 'python_pogoda:test', description: 'Wprowadz nazwe dla budowanego obrazu oraz wersje')
    }
    environment {
        workDir = "./cwiczenia/pogoda_python_flusk"
        myDockerCredential = credentials('dockerHubCredentials')
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
        stage('Wybrales python-flusk') {
            when {
                expression { params.whichScript == 'python-flusk' }
            }
            steps {
                echo "Zbudujemy obraz docker"
            }
        }
        stage('Budowanie Obrazy') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS' && params.whichScript == 'python-flusk'
                }
            }
            steps {
                sh """
                    docker build -t "${localImageName}" "${workDir}"
                """
            }
        }
        stage('Uruchomienie kontenera') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS' && params.whichScript == 'python-flusk'
                }
            }
            steps {
                sh """
                    docker run -d \
                    --name pogoda_api \
                    -p 8000:8000 \
                    -e OPENWEATHER_API_KEY="${myApiKey}" \
                    "${localImageName}"
                    sleep 10
                """
            }
        }
        stage('Test kontenera') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS' && params.whichScript == 'python-flusk'
                }
            }
            steps {
                sh """
                    set -e
                    echo "Testowanie endpointu /weather..."
                    curl -f "http://localhost:8000/weather?city=Warsaw&api_key=${myApiKey}"
                """
            }
        }
        stage('Zatrzymanie i usuniecie kontenera') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS' && params.whichScript == 'python-flusk'
                }
            }
            steps {
                sh """
                    docker stop pogoda_api || true
                    docker rm pogoda_api || true
                """
            }
        }
        stage('Logowanie do docker`a') {
            when {
                expression {
                    currentBuild.currentResult == 'SUCCESS'
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerHubCredentials',
                                                        passwordVariable: 'Password',
                                                        usernameVariable: 'Username')]) {
                sh """
                    sh 'echo "$Username"'
                """
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}