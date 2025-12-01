#!/bin/bash

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

CURRENT_DIR=${PWD##*/}

echo -e "${BLUE}=== Marketplace Quick Start ===${NC}"

if [ -f .env.example ] && [ ! -f .env ]; then
    echo -e "${GREEN}[1/5] Creating .env file from .env.example...${NC}"
    cp .env.example .env
else
    echo -e "${GREEN}[1/5] .env file already exists.${NC}"
fi

cd ..

clone_repo() {
    SERVICE_NAME=$1
    REPO_URL=$2

    if [ ! -d "$SERVICE_NAME" ]; then
        echo -e "${GREEN}[Checking] Cloning $SERVICE_NAME...${NC}"
        git clone "$REPO_URL"
    else
        echo -e "${BLUE}[Skipping] $SERVICE_NAME directory exists.${NC}"
    fi
}

echo -e "${GREEN}[2/5] Checking User Service...${NC}"
clone_repo "user-service" "https://github.com/Marketplace-internship-project/user-service.git"

echo -e "${GREEN}[3/5] Checking Authentication Service...${NC}"
clone_repo "authentication-service" "https://github.com/Marketplace-internship-project/authentication-service.git"

echo -e "${GREEN}[4/5] Checking Order Service...${NC}"
clone_repo "order-service" "https://github.com/Marketplace-internship-project/order-service.git"

cd "$CURRENT_DIR"

echo -e "${GREEN}[5/5] Starting all services with Docker Compose...${NC}"
echo -e "${BLUE}Make sure Docker Desktop is running!${NC}"

docker-compose up --build