package ua.edu.ucu.ds;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ua.edu.ucu.AppendResponseCode;

@Builder
@Setter
@Getter
public class FailureInformation {
    private Integer logId;
    private ReplicationStatus replicationStatus;
    private String failureReason;
    private AppendResponseCode appendResponseCode;
    private Integer attempt;
}
