package com.rajneeti.service.impl;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.rajneeti.dto.profile.ProfileResponse;
import com.rajneeti.dto.profile.StatisticsResponse;
import com.rajneeti.dto.profile.UpdateProfileRequest;
import com.rajneeti.entity.Statistics;
import com.rajneeti.entity.User;
import com.rajneeti.exception.ResourceNotFoundException;
import com.rajneeti.exception.UsernameAlreadyExistsException;
import com.rajneeti.repository.StatisticsRepository;
import com.rajneeti.repository.UserRepository;
import com.rajneeti.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of {@link ProfileService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final StatisticsRepository statisticsRepository;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        User user = findUserById(userId);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        String newUsername = request.getUsername().trim();

        // Check if username changed and is unique
        if (!newUsername.equalsIgnoreCase(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new UsernameAlreadyExistsException("Username '%s' is already taken.".formatted(newUsername));
            }
            log.info("User '{}' updated username to '{}'", user.getUsername(), newUsername);
            user.setUsername(newUsername);
        }

        // Update avatar URL (sanitizing blank to null)
        String newAvatarUrl = request.getAvatarUrl();
        if (newAvatarUrl != null) {
            newAvatarUrl = newAvatarUrl.trim();
            if (newAvatarUrl.isEmpty()) {
                newAvatarUrl = null;
            }
        }
        user.setAvatarUrl(newAvatarUrl);

        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user ID: {}", userId);

        return mapToProfileResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(UUID userId) {
        User user = findUserById(userId);

        Statistics stats = statisticsRepository.findByUserId(userId).orElse(null);

        Integer totalMatches = (stats != null) ? stats.getTotalMatches() : user.getTotalMatches();
        Integer wins = (stats != null) ? stats.getWins() : user.getWins();
        Integer losses = (stats != null) ? stats.getLosses() : user.getLosses();
        Double winRate = (stats != null) ? stats.getWinRate() : calculateWinRate(wins, totalMatches);
        Long coinsEarned = (stats != null) ? stats.getTotalCoinsEarned() : 0L;
        Long coinsSpent = (stats != null) ? stats.getTotalCoinsSpent() : 0L;

        return StatisticsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .rating(user.getRating())
                .totalMatches(totalMatches)
                .wins(wins)
                .losses(losses)
                .winRate(winRate)
                .totalCoinsEarned(coinsEarned)
                .totalCoinsSpent(coinsSpent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateStatisticsPdf(UUID userId) {
        User user = findUserById(userId);
        Statistics stats = statisticsRepository.findByUserId(userId).orElse(null);

        Integer totalMatches = (stats != null) ? stats.getTotalMatches() : user.getTotalMatches();
        Integer wins        = (stats != null) ? stats.getWins()         : user.getWins();
        Integer losses      = (stats != null) ? stats.getLosses()       : user.getLosses();
        Double  winRate     = (stats != null) ? stats.getWinRate()      : calculateWinRate(wins, totalMatches);
        Long    coinsEarned = (stats != null) ? stats.getTotalCoinsEarned() : 0L;
        Long    coinsSpent  = (stats != null) ? stats.getTotalCoinsSpent()  : 0L;
        LocalDateTime updatedAt = (stats != null) ? stats.getUpdatedAt() : LocalDateTime.now();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 60, 60, 60, 60);
            PdfWriter.getInstance(document, out);
            document.open();

            // ── Colour palette ────────────────────────────────────────────
            Color darkGreen  = new Color(30,  90,  60);
            Color lightGrey  = new Color(200, 200, 200);
            Color darkText   = new Color(30,  30,  30);
            Color mutedText  = new Color(100, 100, 100);

            // ── Fonts ─────────────────────────────────────────────────────
            Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  22, darkGreen);
            Font subtitleFont= FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, darkGreen);
            Font labelFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, darkText);
            Font valueFont   = FontFactory.getFont(FontFactory.HELVETICA,       10, darkText);
            Font playerFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  14, darkText);
            Font footerFont  = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, mutedText);
            Font ruleFont    = FontFactory.getFont(FontFactory.HELVETICA,        6, lightGrey);

            // ── Divider helper ────────────────────────────────────────────
            String rule = "─".repeat(70);

            // ── Title block ───────────────────────────────────────────────
            Paragraph title = new Paragraph("RAJNEETI", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph subtitle = new Paragraph("PLAYER STATISTICS", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            Paragraph topRule = new Paragraph(rule, ruleFont);
            topRule.setAlignment(Element.ALIGN_CENTER);
            topRule.setSpacingAfter(14);
            document.add(topRule);

            // ── Player name ───────────────────────────────────────────────
            Paragraph playerLabel = new Paragraph("Player:", labelFont);
            playerLabel.setSpacingAfter(2);
            document.add(playerLabel);

            Paragraph playerName = new Paragraph(user.getUsername(), playerFont);
            playerName.setSpacingAfter(12);
            document.add(playerName);

            // ── Mid rule ──────────────────────────────────────────────────
            Paragraph midRule = new Paragraph(rule, ruleFont);
            midRule.setAlignment(Element.ALIGN_CENTER);
            midRule.setSpacingAfter(12);
            document.add(midRule);

            // ── Statistics table (label / value, 2 columns) ───────────────
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{40f, 60f});
            table.setSpacingAfter(14);

            addStatRow(table, "Total Matches:",     String.valueOf(totalMatches),                labelFont, valueFont);
            addStatRow(table, "Wins:",               String.valueOf(wins),                        labelFont, valueFont);
            addStatRow(table, "Losses:",             String.valueOf(losses),                      labelFont, valueFont);
            addStatRow(table, "Win Rate:",           String.format("%.2f%%", winRate),            labelFont, valueFont);
            addStatRow(table, "Total Coins Earned:", String.valueOf(coinsEarned),                 labelFont, valueFont);
            addStatRow(table, "Total Coins Spent:",  String.valueOf(coinsSpent),                  labelFont, valueFont);
            addStatRow(table, "Last Updated:",
                    updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),            labelFont, valueFont);

            document.add(table);

            // ── Bottom rule ───────────────────────────────────────────────
            Paragraph bottomRule = new Paragraph(rule, ruleFont);
            bottomRule.setAlignment(Element.ALIGN_CENTER);
            bottomRule.setSpacingAfter(8);
            document.add(bottomRule);

            // ── Footer ────────────────────────────────────────────────────
            Paragraph footer1 = new Paragraph("Rajneeti", footerFont);
            footer1.setAlignment(Element.ALIGN_CENTER);
            footer1.setSpacingAfter(2);
            document.add(footer1);

            Paragraph footer2 = new Paragraph("A Real-Time Multiplayer Bluff Strategy Game", footerFont);
            footer2.setAlignment(Element.ALIGN_CENTER);
            document.add(footer2);

            document.close();
            log.info("Statistics PDF generated ({} bytes) for user ID: {}", out.size(), userId);
            return out.toByteArray();

        } catch (DocumentException | java.io.IOException ex) {
            throw new RuntimeException("Failed to generate statistics PDF for user: " + userId, ex);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Adds a labelled row to the statistics table. */
    private static void addStatRow(PdfPTable table, String label, String value,
                                    Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.BOTTOM);
        labelCell.setBorderColorBottom(new Color(220, 220, 220));
        labelCell.setPadding(6);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.BOTTOM);
        valueCell.setBorderColorBottom(new Color(220, 220, 220));
        valueCell.setPadding(6);
        table.addCell(valueCell);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private ProfileResponse mapToProfileResponse(User user) {
        Double winRate = calculateWinRate(user.getWins(), user.getTotalMatches());

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .rating(user.getRating())
                .totalMatches(user.getTotalMatches())
                .wins(user.getWins())
                .losses(user.getLosses())
                .winRate(winRate)
                .build();
    }

    private Double calculateWinRate(Integer wins, Integer totalMatches) {
        if (totalMatches != null && totalMatches > 0 && wins != null) {
            return Math.round(((double) wins / totalMatches * 100.0) * 100.0) / 100.0;
        }
        return 0.0;
    }
}