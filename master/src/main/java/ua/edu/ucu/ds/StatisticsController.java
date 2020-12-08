package ua.edu.ucu.ds;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("admin")
public class StatisticsController {
    @Autowired LogReplicatorService logReplicatorService;

    @GetMapping(path = "/failure-statistics", produces = "application/json")
    public Map<String,
                List<FailureInformation>> getBook() {
        return logReplicatorService.getFailureStatistics();
    }
}
