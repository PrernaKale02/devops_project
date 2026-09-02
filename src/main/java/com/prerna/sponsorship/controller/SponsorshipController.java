package com.prerna.sponsorship.controller;

import com.prerna.sponsorship.model.Sponsorship;
import com.prerna.sponsorship.repository.SponsorshipRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            sponsorships = repository
                    .findByChildNameContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(
                            search, search);
        } else {
            sponsorships = repository.findAll();
        }

        model.addAttribute("sponsorships", sponsorships);
        model.addAttribute("search", search);

        return "sponsorships";
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