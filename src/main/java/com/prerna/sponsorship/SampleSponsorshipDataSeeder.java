package com.prerna.sponsorship;

import com.prerna.sponsorship.model.Sponsorship;
import com.prerna.sponsorship.repository.SponsorshipRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class SampleSponsorshipDataSeeder implements CommandLineRunner {

    private final SponsorshipRepository repository;

    public SampleSponsorshipDataSeeder(SponsorshipRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        for (Sponsorship sponsorship : sampleRecords()) {
            if (!repository.existsByChildId(sponsorship.getChildId())) {
                repository.save(sponsorship);
            }
        }
    }

    private List<Sponsorship> sampleRecords() {
        return List.of(
                create("CH-1001", "Aarav Patil", 10, "Male", "Primary", "Sunrise Public School", "Rahul Mehta (Demo)",
                        25000, LocalDate.of(2025, 6, 1), Sponsorship.Status.ACTIVE),
                create("CH-1002", "Ananya Sharma", 13, "Female", "Secondary", "Vidya Niketan School",
                        "Neha Kapoor (Demo)", 32000, LocalDate.of(2025, 8, 15), Sponsorship.Status.ACTIVE),
                create("CH-1003", "Rohan Jadhav", 8, "Male", "Primary", "New Horizon Academy", "Amit Desai (Demo)",
                        18000, LocalDate.of(2026, 2, 10), Sponsorship.Status.PENDING),
                create("CH-1004", "Isha Kulkarni", 15, "Female", "Secondary", "Starlight English School",
                        "Priya Shah (Demo)", 40000, LocalDate.of(2023, 6, 1), Sponsorship.Status.COMPLETED),
                create("CH-1005", "Kabir Joshi", 11, "Male", "Primary", "Green Valley School", "Arjun Malhotra (Demo)",
                        27500, LocalDate.of(2025, 11, 1), Sponsorship.Status.ACTIVE),
                create("CH-1006", "Meera More", 16, "Female", "Higher Secondary", "Maharashtra Vidya Mandal",
                        "Sneha Rao (Demo)", 45000, LocalDate.of(2026, 3, 20), Sponsorship.Status.PENDING),
                create("CH-1007", "Vivaan Deshmukh", 14, "Male", "Secondary", "Knowledge Springs School",
                        "Karan Sethi (Demo)", 35000, LocalDate.of(2024, 1, 15), Sponsorship.Status.CANCELLED),
                create("CH-1008", "Siya Nair", 17, "Female", "Higher Secondary", "Bright Future Junior College",
                        "Riya Menon (Demo)", 50000, LocalDate.of(2023, 6, 1), Sponsorship.Status.COMPLETED));
    }

    private Sponsorship create(
            String childId,
            String childName,
            int age,
            String gender,
            String educationLevel,
            String school,
            String sponsorName,
            double amount,
            LocalDate startDate,
            Sponsorship.Status status) {
        Sponsorship sponsorship = new Sponsorship();
        sponsorship.setChildId(childId);
        sponsorship.setChildName(childName);
        sponsorship.setAge(age);
        sponsorship.setGender(gender);
        sponsorship.setEducationLevel(educationLevel);
        sponsorship.setSchool(school);
        sponsorship.setSponsorName(sponsorName);
        sponsorship.setSponsorshipAmount(amount);
        sponsorship.setStartDate(startDate);
        sponsorship.setStatus(status);
        return sponsorship;
    }
}
