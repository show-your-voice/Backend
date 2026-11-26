package com.example.showyourvoice.domain.place.controller.dto;

import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.user.domain.User;

public record AssignmentRuleResponse(Long roomId, String roomName, String placeName, PostCategory category,
                                     Long teacherId, String teacherName) {

    public static AssignmentRuleResponse from(AssignmentRule rule) {
        Room room = rule.getRoom();
        User teacher = rule.getTeacher();
        return new AssignmentRuleResponse(room.getId(), room.getName(), room.getPlace().getName(),
                rule.getCategory(), teacher.getId(), teacher.getName());
    }
}
