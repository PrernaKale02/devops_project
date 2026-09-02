package com.prerna.sponsorship.repository;

import com.prerna.sponsorship.model.Sponsorship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {

    List<Sponsorship> findByChildNameContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(
            String childName,
            String sponsorName);
}