# Ansible-Provisioned Child Education Sponsorship System

**Name:** Prerna Navnit Kale  
**Roll Number:** 24102B2001  
**Class:** BE CMPN B

## Project
A small web-based Child Education Sponsorship System designed to centralize sponsorship records and demonstrate a complete DevOps lifecycle.

## MVP
- User login and role-based access
- Sponsorship record creation, viewing, updating and searching
- Pending → Active → Completed status workflow
- Cancelled status for authorized users
- Summary dashboard
- Git/GitHub, Jenkins, Selenium, Docker and Ansible

## Technology Stack
- Java 21
- Spring Boot
- Maven
- Thymeleaf
- H2 for initial local development
- MySQL planned for deployment
- Selenium WebDriver
- Jenkins
- Docker
- Ansible

## Run locally
```bash
mvn clean test
mvn spring-boot:run
```

Open http://localhost:8080

H2 console: http://localhost:8080/h2-console

## Planned DevOps flow
GitHub → Jenkins → Maven Build → Selenium → Docker → Deployment → Ansible → Health Check
