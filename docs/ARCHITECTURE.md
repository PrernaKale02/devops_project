# System Architecture

```text
+----------------------+
|      Browser         |
+----------+-----------+
           |
           v
+----------------------+
| Spring Boot Web App  |
| Controllers/Services |
+----------+-----------+
           |
     +-----+------+
     |            |
     v            v
+---------+   +---------+
| Thymeleaf|   |  JPA   |
|   UI     |   | Layer  |
+---------+   +----+----+
                   |
                   v
             +-----------+
             | Database  |
             | H2 local  |
             | MySQL later|
             +-----------+

DevOps:
GitHub → Jenkins → Maven → Selenium → Docker → Deploy → Ansible
```

## Layers
- Presentation: Thymeleaf pages
- Controller: HTTP request handling
- Service: business rules
- Repository: database access
- Database: sponsorship and user records
