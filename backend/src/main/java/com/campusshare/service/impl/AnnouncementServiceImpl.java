package com.campusshare.service.impl;

import com.campusshare.dto.AnnouncementRequest;
import com.campusshare.entity.Announcement;
import com.campusshare.exception.BusinessException;
import com.campusshare.mapper.AnnouncementMapper;
import com.campusshare.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> list(String keyword) {
        return announcementMapper.findAll(keyword);
    }

    @Override
    @Transactional
    public Announcement create(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setSummary(request.getSummary());
        announcement.setContent(request.getContent());
        announcement.setPinned(request.getPinned());
        announcement.setPublishAt(LocalDateTime.now());
        announcementMapper.insert(announcement);
        return announcement;
    }

    @Override
    @Transactional
    public Announcement update(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementMapper.findById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        announcement.setTitle(request.getTitle());
        announcement.setSummary(request.getSummary());
        announcement.setContent(request.getContent());
        announcement.setPinned(request.getPinned());
        announcementMapper.update(announcement);
        return announcement;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (announcementMapper.delete(id) == 0) {
            throw new BusinessException("删除公告失败");
        }
    }
}

