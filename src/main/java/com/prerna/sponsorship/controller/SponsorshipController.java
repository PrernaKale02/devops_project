package com.prerna.sponsorship.controller;

import com.prerna.sponsorship.model.Sponsorship;
import com.prerna.sponsorship.repository.SponsorshipRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/sponsorships")
public class SponsorshipController {

    private final SponsorshipRepository repository;

    public SponsorshipController(SponsorshipRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            Model model) {

        List<Sponsorship> sponsorships;

        if (search != null && !search.trim().isEmpty()) {
            String normalizedSearch = search.trim();
            sponsorships = repository
                    .findByChildIdContainingIgnoreCaseOrChildNameContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(
                            normalizedSearch, normalizedSearch, normalizedSearch);

            try {
                Sponsorship.Status status = Sponsorship.Status.valueOf(
                        normalizedSearch.toUpperCase(Locale.ROOT));
                sponsorships = repository.findByStatus(status);
            } catch (IllegalArgumentException ignored) {
                // Text search covers child and sponsor fields.
            }
        } else {
            sponsorships = repository.findAll();
        }

        model.addAttribute("sponsorships", sponsorships);
        model.addAttribute("search", search);

        return "sponsorships";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCount", repository.count());
        model.addAttribute("pendingCount", repository.countByStatus(Sponsorship.Status.PENDING));
        model.addAttribute("activeCount", repository.countByStatus(Sponsorship.Status.ACTIVE));
        model.addAttribute("completedCount", repository.countByStatus(Sponsorship.Status.COMPLETED));
        model.addAttribute("cancelledCount", repository.countByStatus(Sponsorship.Status.CANCELLED));
        return "dashboard";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        Sponsorship sponsorship = new Sponsorship();
        sponsorship.setStatus(Sponsorship.Status.PENDING);

        model.addAttribute("sponsorship", sponsorship);
        return "sponsorship-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Sponsorship sponsorship) {
        if (sponsorship.getStatus() == null) {
            sponsorship.setStatus(Sponsorship.Status.PENDING);
        }

        repository.save(sponsorship);
        return "redirect:/sponsorships";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Sponsorship.Status status) {
        Sponsorship sponsorship = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid sponsorship ID: " + id));
        sponsorship.setStatus(status);
        repository.save(sponsorship);
        return "redirect:/sponsorships";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id, Model model) {
        Sponsorship sponsorship = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid sponsorship ID: " + id));

        model.addAttribute("sponsorship", sponsorship);
        return "sponsorship-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        repository.deleteById(id);
        return "redirect:/sponsorships";
    }
}