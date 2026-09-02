# Use-Case Model

## Actors
Administrator, Sponsorship Coordinator, Sponsor

## Use Cases
Administrator:
- Login
- Create users
- Manage sponsorships
- Manage statuses
- View dashboard

Coordinator:
- Login
- Create sponsorship
- View sponsorship
- Update sponsorship
- Search sponsorship
- Pending → Active

Sponsor:
- Login
- View own sponsorship records

## Mermaid Diagram
```mermaid
flowchart LR
    A[Administrator] --> L[Login]
    A --> U[Manage Users]
    A --> S[Manage Sponsorships]
    A --> W[Manage Status]
    A --> D[View Dashboard]

    C[Sponsorship Coordinator] --> L
    C --> S
    C --> P[Pending to Active]
    C --> Q[Search Records]

    SP[Sponsor] --> L
    SP --> O[View Own Sponsorships]
```
