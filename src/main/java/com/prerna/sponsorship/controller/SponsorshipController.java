package com.prerna.sponsorship.controller;

import com.prerna.sponsorship.model.Sponsorship;
import com.prerna.sponsorship.repository.SponsorshipRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String educationLevel,
            Model model) {

        List<Sponsorship> sponsorships;

        if (search != null && !search.trim().isEmpty()) {
            String normalizedSearch = search.trim();
            sponsorships = repository
                    .findByChildIdContainingIgnoreCaseOrChildNameContainingIgnoreCaseOrSponsorNameContainingIgnoreCase(
                            normalizedSearch, normalizedSearch, normalizedSearch);

            try {
                Sponsorship.Status parsedStatus = Sponsorship.Status.valueOf(
                        normalizedSearch.toUpperCase(Locale.ROOT));
                sponsorships = repository.findByStatus(parsedStatus);
            } catch (IllegalArgumentException ignored) {
                // Text search covers child and sponsor fields.
            }
        } else {
            sponsorships = repository.findAll();
        }

        if (status != null && !status.isBlank()) {
            sponsorships = sponsorships.stream().filter(s -> s.getStatus() != null && s.getStatus().name().equals(status)).toList();
        }
        if (educationLevel != null && !educationLevel.isBlank()) {
            sponsorships = sponsorships.stream().filter(s -> educationLevel.equalsIgnoreCase(s.getEducationLevel())).toList();
        }

        model.addAttribute("sponsorships", sponsorships);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedEducationLevel", educationLevel);
        model.addAttribute("statuses", Sponsorship.Status.values());
        model.addAttribute("educationLevels", repository.findAll().stream().map(Sponsorship::getEducationLevel).filter(v -> v != null && !v.isBlank()).distinct().sorted().toList());

        return "sponsorships";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Sponsorship> records = repository.findAll();
        model.addAttribute("totalCount", records.stream().map(Sponsorship::getChildId).distinct().count());
        model.addAttribute("sponsorshipCount", records.size());
        model.addAttribute("totalAmount", records.stream().mapToDouble(Sponsorship::getSponsorshipAmount).sum());
        model.addAttribute("pendingCount", repository.countByStatus(Sponsorship.Status.PENDING));
        model.addAttribute("activeCount", repository.countByStatus(Sponsorship.Status.ACTIVE));
        model.addAttribute("completedCount", repository.countByStatus(Sponsorship.Status.COMPLETED));
        model.addAttribute("cancelledCount", repository.countByStatus(Sponsorship.Status.CANCELLED));
        model.addAttribute("recentSponsorships", repository.findTop6ByOrderByStartDateDesc());
        return "dashboard";
    }

    @GetMapping("/children")
    public String children(@RequestParam(required = false) String search, Model model) {
        List<Sponsorship> records = repository.findAll();
        Map<String, Sponsorship> children = new LinkedHashMap<>();
        records.stream().sorted(Comparator.comparing(Sponsorship::getId)).forEach(s -> children.putIfAbsent(s.getChildId(), s));
        List<Sponsorship> result = new ArrayList<>(children.values());
        if (search != null && !search.isBlank()) result = result.stream().filter(s -> contains(s.getChildId(), search) || contains(s.getChildName(), search) || contains(s.getSchool(), search)).toList();
        model.addAttribute("children", result);
        model.addAttribute("search", search);
        return "children";
    }

    @GetMapping("/sponsors")
    public String sponsors(@RequestParam(required = false) String search, Model model) {
        Map<String, List<Sponsorship>> groups = repository.findAll().stream().collect(Collectors.groupingBy(Sponsorship::getSponsorName, LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> sponsors = groups.entrySet().stream().map(e -> {
            List<Sponsorship> rows = e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", e.getKey()); row.put("count", rows.size());
            row.put("active", rows.stream().filter(s -> s.getStatus() == Sponsorship.Status.ACTIVE).count());
            row.put("total", rows.stream().mapToDouble(Sponsorship::getSponsorshipAmount).sum());
            row.put("id", rows.get(0).getId()); return row;
        }).filter(s -> search == null || search.isBlank() || contains(String.valueOf(s.get("name")), search)).toList();
        model.addAttribute("sponsors", sponsors); model.addAttribute("search", search);
        return "sponsors";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        model.addAttribute("sponsorship", repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid sponsorship ID: " + id)));
        return "sponsorship-details";
    }

    private boolean contains(String value, String query) { return value != null && value.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT)); }

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
