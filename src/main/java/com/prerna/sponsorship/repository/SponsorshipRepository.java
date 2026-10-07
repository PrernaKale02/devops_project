package com.prerna.sponsorship.repository;

import com.prerna.sponsorship.model.Sponsorship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SponsorshipRepository extends JpaRepository<Sponsorship, Long> {

    boolean existsByChildId(String childId);

    List<Sponsorship> findByChildIdContainingIgnoreCaseOrChildNameContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(
            String childId,
            String childName,
            String sponsorName);

    List<Sponsorship> findByStatus(Sponsorship.Status status);

    long countByStatus(Sponsorship.Status status);

    List<Sponsorship> findTop6ByOrderByStartDateDesc();
}
