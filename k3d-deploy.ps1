# Phase 10: Scalability Proof Infrastructure Deployment Script

Write-Host "=========================================="
Write-Host "CMS Phase 10 Scalability Proof Deployment"
Write-Host "=========================================="

# 1. Create k3d cluster
# Write-Host "Cleaning up any existing cluster..."
# k3d cluster delete cms-cluster
# 
# Write-Host "Creating k3d cluster (1 master, 3 agents)..."
# k3d cluster create cms-cluster --servers 1 --agents 3 -p "80:80@loadbalancer" -p "443:443@loadbalancer"
# 
# Write-Host "Waiting for cluster to be ready..."
# Start-Sleep -Seconds 10
# kubectl get nodes

# 2. Deploy MySQL Cluster (1 Primary, 2 Replicas)
Write-Host "Deploying MySQL Primary and Replicas..."
kubectl apply -f k8s/mysql-configmap.yaml
kubectl apply -f k8s/mysql-primary.yaml
kubectl apply -f k8s/mysql-replica.yaml

# 3. Add Bitnami Helm Repo
Write-Host "Adding Bitnami Helm Repository..."
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# 4. Deploy Kafka (3 Brokers)
Write-Host "Deploying Kafka Cluster..."
helm install cms-kafka bitnami/kafka -f kafka-values.yaml

# 5. Deploy Redis (Sentinel + Replica)
Write-Host "Deploying Redis Sentinel..."
helm install cms-redis bitnami/redis -f redis-values.yaml

# 6. Deploy CMS Application Helm Chart
Write-Host "Deploying CMS Microservices..."
helm install cms ./helm/cms

Write-Host "=========================================="
Write-Host "Deployment initiated successfully!"
Write-Host "Run 'kubectl get pods -w' to monitor the startup progress."
Write-Host "=========================================="
