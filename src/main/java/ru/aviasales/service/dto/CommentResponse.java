package ru.aviasales.service.dto;

import lombok.Data;
import ru.aviasales.dal.model.Comment;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private LocalDateTime createdAt;
    private String moderationComment;

    public static CommentResponse fromEntity(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setModerationComment(comment.getModerationComment());
        return response;
    }
}
