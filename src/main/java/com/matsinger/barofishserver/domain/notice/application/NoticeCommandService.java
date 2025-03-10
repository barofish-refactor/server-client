package com.matsinger.barofishserver.domain.notice.application;

import com.matsinger.barofishserver.domain.notice.domain.Notice;
import com.matsinger.barofishserver.domain.notice.dto.NoticeAddReq;
import com.matsinger.barofishserver.domain.notice.repository.NoticeRepository;
import com.matsinger.barofishserver.utils.Common;
import com.matsinger.barofishserver.utils.S3.S3Uploader;
import com.matsinger.barofishserver.utils.S3.Uploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class NoticeCommandService {
    private final NoticeRepository noticeRepository;
    private final NoticeQueryService noticeQueryService;
    private final Common utils;
    private final Uploader s3;

    public Notice addNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    public Notice updateNotice(Integer noticeId, NoticeAddReq request) {

        Notice notice = noticeQueryService.selectNotice(noticeId);
        if (request.getTitle() != null) {
            String title = utils.validateString(request.getTitle(), 200L, "제목");
            notice.setTitle(title);
        }

        List<Notice> notices = noticeRepository.findAll();
        if (request.getIsRepresentative() == true) {
            findExistingRepresentativeNoticeAndSetFalse(notices);
            notice.setRepresentative(true);
        }
        if (request.getIsRepresentative() == false) {
            notice.setRepresentative(false);
        }

        if (request.getContent() != null) {
            String
                    content =
                    s3.uploadEditorStringToS3(request.getContent(),
                            new ArrayList<>(Arrays.asList("notice", String.valueOf(notice.getId()))));
            notice.setContent(content);
        }
        notice.setUpdateAt(utils.now());

        return noticeRepository.save(notice);
    }

    private static void findExistingRepresentativeNoticeAndSetFalse(List<Notice> notices) {
        for (Notice existingNotice : notices) {
            if (existingNotice.isRepresentative() == true) {
                existingNotice.setRepresentative(false);
            }
        }
    }

    public Boolean deleteNotice(Integer noticeId) {
        try {
            noticeRepository.deleteById(noticeId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
