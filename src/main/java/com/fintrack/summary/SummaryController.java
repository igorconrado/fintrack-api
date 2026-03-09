package com.fintrack.summary;

import com.fintrack.summary.dto.MonthlySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(@RequestParam Integer month,
                                                                    @RequestParam Integer year) {
        return ResponseEntity.ok(summaryService.getMonthlySummary(month, year));
    }
}
