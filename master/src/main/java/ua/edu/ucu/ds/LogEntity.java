package ua.edu.ucu.ds;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;

@Builder
@Getter
public class LogEntity {
    private Integer id;
    private String log;
    private Integer writeConcern;

}
