# ==============================================================================
# IncidentIQ — AWS Production Infrastructure as Code (Terraform Reference)
# ==============================================================================

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.40"
    }
  }
}

provider "aws" {
  region = var.aws_region
  default_tags {
    tags = {
      Project     = "IncidentIQ"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

variable "aws_region" {
  default = "us-east-1"
}

variable "environment" {
  default = "production"
}

variable "vpc_cidr" {
  default = "10.0.0.0/16"
}

# ------------------------------------------------------------------------------
# 1. VPC & Multi-AZ Networking
# ------------------------------------------------------------------------------
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "incidentiq-${var.environment}-vpc"
  }
}

resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.aws_region}a"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "public_b" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.aws_region}b"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "private_app_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "${var.aws_region}a"
}

resource "aws_subnet" "private_app_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.12.0/24"
  availability_zone = "${var.aws_region}b"
}

resource "aws_subnet" "private_data_a" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.21.0/24"
  availability_zone = "${var.aws_region}a"
}

resource "aws_subnet" "private_data_b" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.22.0/24"
  availability_zone = "${var.aws_region}b"
}

# ------------------------------------------------------------------------------
# 2. Amazon RDS PostgreSQL 16 with pgvector Extension
# ------------------------------------------------------------------------------
resource "aws_db_subnet_group" "rds" {
  name       = "incidentiq-${var.environment}-rds-subnet-group"
  subnet_ids = [aws_subnet.private_data_a.id, aws_subnet.private_data_b.id]
}

resource "aws_db_parameter_group" "pg16_vector" {
  name   = "incidentiq-${var.environment}-pg16-vector"
  family = "postgres16"

  parameter {
    name  = "shared_preload_libraries"
    value = "vector"
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "incidentiq-${var.environment}-postgres"
  engine                 = "postgres"
  engine_version         = "16.2"
  instance_class         = "db.m6g.large"
  allocated_storage      = 100
  max_allocated_storage  = 1000
  storage_type           = "gp3"
  multi_az               = true
  db_name                = "incidentiq"
  username               = "incidentiq_master"
  manage_master_user_password = true
  db_subnet_group_name   = aws_db_subnet_group.rds.name
  parameter_group_name   = aws_db_parameter_group.pg16_vector.name
  skip_final_snapshot    = false
  final_snapshot_identifier = "incidentiq-db-final-snapshot"
  deletion_protection    = true
}

# ------------------------------------------------------------------------------
# 3. Amazon ElastiCache Redis 7
# ------------------------------------------------------------------------------
resource "aws_elasticache_subnet_group" "redis" {
  name       = "incidentiq-${var.environment}-redis-subnet-group"
  subnet_ids = [aws_subnet.private_data_a.id, aws_subnet.private_data_b.id]
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "incidentiq-${var.environment}-redis"
  description                = "IncidentIQ Redis cluster for session cache and rate limiting"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = "cache.m6g.large"
  num_cache_clusters         = 2
  parameter_group_name       = "default.redis7"
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.redis.name
  automatic_failover_enabled = true
  transit_encryption_enabled = true
  at_rest_encryption_enabled = true
}

# ------------------------------------------------------------------------------
# 4. Amazon ECS Cluster & Fargate Services
# ------------------------------------------------------------------------------
resource "aws_ecs_cluster" "main" {
  name = "incidentiq-${var.environment}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# ------------------------------------------------------------------------------
# 5. Application Load Balancer
# ------------------------------------------------------------------------------
resource "aws_lb" "alb" {
  name               = "incidentiq-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_b.id]
}
