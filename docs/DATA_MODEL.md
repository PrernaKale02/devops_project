# Data Model

## User
- id
- name
- username
- password
- role

## Sponsorship
- id
- childId
- childName
- age
- gender
- educationLevel
- school
- sponsorName
- sponsorshipAmount
- startDate
- status

## Status
Pending → Active → Completed
Cancelled is an authorized terminal state.

## Relationships
A user with Sponsor role can view sponsorship records associated with that sponsor. Administrators and coordinators have broader access according to their role.
