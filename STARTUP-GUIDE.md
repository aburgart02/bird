# Руководство по запуску

### Обязательное ПО:
- **Docker Desktop** (или Docker Engine)
- **Minikube** v1.30+
- **kubectl** v1.27+
- **Node.js** v18+ и npm
- **Java JDK 21**
- **Maven** 3.8+

### Проверка установки:
```bash
docker --version
minikube version
kubectl version --client
node --version
java --version
mvn --version
```

---

## Запуск в Kubernetes

### 1. Запуск Minikube

```bash
# Запуск кластера с Docker драйвером
minikube start --driver=docker --memory=4096 --cpus=2

# Включение Ingress контроллера
minikube addons enable ingress

# Проверка статуса
minikube status
```

### 2. Настройка Docker для Minikube

```bash
# Для Windows PowerShell:
& minikube -p minikube docker-env --shell powershell | Invoke-Expression
```

### 3. Сборка приложений

```bash
cd c:\Users\aburg\Desktop\bird

# Сборка Java приложений
mvn clean package -DskipTests

# Сборка Docker образов
docker build -t bird-ums:latest ./ums
docker build -t bird-twitter:latest ./twitter

# Сборка фронтенда с нужным API URL
docker build -t bird-front:latest --build-arg VITE_API_URL=/api/ums ./front
```

### 4. Развёртывание в Kubernetes

```bash
# Применить все манифесты
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/db-init-configmap.yaml
kubectl apply -f k8s/db-deployment.yaml

# Подождать пока БД запустится
kubectl wait --for=condition=ready pod -l app=db -n bird --timeout=120s

# Развернуть сервисы
kubectl apply -f k8s/ums-deployment.yaml
kubectl apply -f k8s/twitter-deployment.yaml
kubectl apply -f k8s/front-deployment.yaml

# Применить Ingress
kubectl apply -f k8s/ingress.yaml
```


### 5. Проверка развёртывания
```bash
# Статус подов
kubectl get pods -n bird

# Статус сервисов
kubectl get services -n bird

# Статус Ingress
kubectl get ingress -n bird

# Логи подов
kubectl logs -f deployment/ums -n bird
kubectl logs -f deployment/twitter -n bird
kubectl logs -f deployment/db -n bird
```

### 6. Инициализация БД для twitter
```
kubectl exec -it db-7dcbb9f459-z5w5z -n bird -- mysql -u root -p twitter

DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS subscriptions;
DROP TABLE IF EXISTS producers;
DROP TABLE IF EXISTS subscribers;

CREATE TABLE `producers` (
    `producer_id` BINARY(16) PRIMARY KEY
);

CREATE TABLE `subscribers` (
    `subscriber_id` BINARY(16) PRIMARY KEY
);

CREATE TABLE `messages` (
    `id` BINARY(16) PRIMARY KEY,
    `producer_id` BINARY(16) NOT NULL,
    `content` TEXT NOT NULL,
    `created` BIGINT NOT NULL,
    FOREIGN KEY (`producer_id`) REFERENCES `producers`(`producer_id`) ON DELETE CASCADE
);

CREATE TABLE `subscriptions` (
    `subscriber_id` BINARY(16) NOT NULL,
    `producer_id` BINARY(16) NOT NULL,
    PRIMARY KEY (`subscriber_id`, `producer_id`),
    FOREIGN KEY (`subscriber_id`) REFERENCES `subscribers`(`subscriber_id`) ON DELETE CASCADE,
    FOREIGN KEY (`producer_id`) REFERENCES `producers`(`producer_id`) ON DELETE CASCADE
);

SHOW TABLES;
DESCRIBE messages;

kubectl rollout restart deployment twitter -n bird
```

### 7. Открытие приложения

```bash
# Запуск туннель Minikube
minikube tunnel
```

**http://127.0.0.1**

# Полезные команды
```
#Удаление образа
docker rmi bird-ums:latest

#Загрузка нового образа
minikube image load bird-ums:latest

#Удаление пода
kubectl delete pods -n bird -l app=ums

#Полное удаление приложения из пространства в кластере
kubectl delete deployment front -n bird
```

---

## Локальный запуск без Kubernetes

### 1. Запуск MySQL

```bash
# Запуск контейнера
docker run -d \
  --name bird-mysql \
  -e MYSQL_ROOT_PASSWORD=password \
  -p 3306:3306 \
  mysql:8.0

# Инициализация БД
docker exec -i bird-mysql mysql -uroot -ppassword < database/ums.sql
docker exec -i bird-mysql mysql -uroot -ppassword < database/twitter.sql
docker exec -i bird-mysql mysql -uroot -ppassword < database/github_oauth.sql

# Альтернативный способ инициализации БД
docker cp C:\Users\aburg\Desktop\bird\database\ums.sql bird-mysql:/tmp/ums.sql
docker cp C:\Users\aburg\Desktop\bird\database\twitter.sql bird-mysql:/tmp/twitter.sql

docker exec -it bird-mysql mysql -u root -p

CREATE DATABASE ums;

USE ums;

SOURCE /tmp/ums.sql;

CREATE DATABASE twitter;
USE twitter;
SOURCE /tmp/twitter.sql;
```

### 2. Запуск бэкенда

```bash
cd ums
mvn spring-boot:run

cd twitter
mvn spring-boot:run
```

### 3. Запуск фронтенда

```bash
cd front
npm install
npm run dev
```

**http://localhost:5173**