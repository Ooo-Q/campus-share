package com.campusshare.vo;

import com.campusshare.entity.Resource;
import lombok.Data;

import java.util.List;

@Data
public class AdminOverviewResponse {

    private Stats stats;

    private List<Resource> recentResources;

    @Data
    public static class Stats {
        private long pendingReports;
    }
}
