package com.campusshare.service;

import com.campusshare.dto.AnnouncementRequest;
import com.campusshare.entity.Announcement;

import java.util.List;

public interface AnnouncementService {
    List<Announcement> list(String keyword);

    Announcement create(AnnouncementRequest request);

    Announcement update(Long id, AnnouncementRequest request);

    void delete(Long id);
}
