package com.recipereels.repository;

import com.recipereels.entity.DirectMessage;
import com.recipereels.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findByRecipientOrderByCreatedAtDesc(User recipient);
    List<DirectMessage> findBySenderOrderByCreatedAtDesc(User sender);
    long countByRecipientAndIsReadFalse(User recipient);

    @Query("SELECT m FROM DirectMessage m WHERE (m.sender = :u1 AND m.recipient = :u2) OR (m.sender = :u2 AND m.recipient = :u1) ORDER BY m.createdAt ASC")
    List<DirectMessage> findConversation(@Param("u1") User u1, @Param("u2") User u2);
}
