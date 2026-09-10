package com.campusshare.service.impl;

import com.campusshare.mapper.AdminMapper;
import com.campusshare.service.AdminService;
import com.campusshare.vo.AdminOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;

    @Override
    public AdminOverviewResponse overview() {
        AdminOverviewResponse response = new AdminOverviewResponse();
        AdminOverviewResponse.Stats stats = new AdminOverviewResponse.Stats();
        stats.setPendingReports(adminMapper.countPendingReports());
        response.setStats(stats);
        response.setRecentResources(adminMapper.latestResources(6));
        return response;
    }
}


