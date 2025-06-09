package io.github.glitteringuniverse.controller;

import io.github.glitteringuniverse.dto.WorkOrderStatistics;
import io.github.glitteringuniverse.entity.WorkOrder;
import io.github.glitteringuniverse.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"}, allowCredentials = "true")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/daily")
    public WorkOrderStatistics getDailyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String organizationLevel) {
        return statisticsService.getDailyStatistics(date, department, organizationLevel);
    }
    
    @GetMapping("/weekly")
    public WorkOrderStatistics getWeeklyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String organizationLevel) {
        return statisticsService.getWeeklyStatistics(weekStart, department, organizationLevel);
    }
    
    @GetMapping("/overdue")
    public List<WorkOrder> getOverdueOrders() {
        return statisticsService.getOverdueOrders();
    }
    
    @GetMapping("/soon-to-expire")
    public List<WorkOrder> getSoonToExpireOrders() {
        return statisticsService.getSoonToExpireOrders();
    }
    
    @GetMapping("/overall")
    public WorkOrderStatistics getOverallStatistics(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String organizationLevel) {
        return statisticsService.getOverallStatistics(department, organizationLevel);
    }
}
