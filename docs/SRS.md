# Software Requirements Specification — Summary

## 1. Purpose
The system centralizes child education sponsorship information and provides controlled access to sponsorship records.

## 2. Users
- Administrator: manages users, sponsorship records and statuses.
- Sponsorship Coordinator: manages child and sponsorship records.
- Sponsor: views sponsorship information linked to the sponsor.

## 3. Functional Requirements
FR-01: Authenticate authorized users.
FR-02: Create user accounts and assign roles.
FR-03: Create sponsorship records.
FR-04: View sponsorship records.
FR-05: Update sponsorship records.
FR-06: Search records by Child ID, Child Name and Status.
FR-07: Enforce role-based permissions.
FR-08: Move sponsorships through the defined status workflow.
FR-09: Display sponsorship statistics.
FR-10: Support the planned DevOps build, test, containerization and provisioning lifecycle.

## 4. Data
The system uses mock/sample data only during development.

## 5. Non-functional Requirements
- Simple web interface
- Maintainable modular code
- Role-based access control
- Repeatable build and deployment
- Containerized execution
- Automated testing

## 6. Constraints
The MVP excludes payments, mobile applications, messaging, advanced analytics, AI recommendations and real-time chat.
